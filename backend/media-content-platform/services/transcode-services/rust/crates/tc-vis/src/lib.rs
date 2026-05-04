//! Video inspection — fast probe strategies (VIS analogue).
//!
//! Ported behavior from legacy worker:
//! - try strategies by priority;
//! - skip unsupported formats per strategy;
//! - return first successful probe.

use std::fs::File;
use std::io::{Read, Write};
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

use anyhow::Result;
use tc_runtime::config::RuntimeConfig;
use tc_runtime::storage::StorageClient;
use transcode_contracts::dto::VideoMetadata;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ProbeError {
    Io(String),
    ProbeFailed(String),
    Unsupported(String),
    AllStrategiesFailed(String),
}

impl std::fmt::Display for ProbeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ProbeError::Io(s) => write!(f, "io: {s}"),
            ProbeError::ProbeFailed(s) => write!(f, "probe_failed: {s}"),
            ProbeError::Unsupported(s) => write!(f, "unsupported: {s}"),
            ProbeError::AllStrategiesFailed(s) => write!(f, "all_failed: {s}"),
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct ProbeOutcome {
    pub metadata: VideoMetadata,
    pub strategy: &'static str,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ProbeRequest {
    pub bucket: String,
    pub key: String,
    /// Optional explicit source accepted by ffprobe (path/url).
    /// When absent, strategies resolve source from storage.
    pub source: Option<String>,
}

pub trait ProbeStrategy: Send + Sync {
    fn name(&self) -> &'static str;
    fn priority(&self) -> i32;
    fn supports(&self, request: &ProbeRequest) -> bool;
    fn probe(&self, request: &ProbeRequest) -> Result<ProbeOutcome, ProbeError>;
}

/// Preferred strategy: ffprobe reads directly from source (URL/path).
pub struct PresignedUrlProbe {
    pub ffprobe_path: String,
    pub storage: StorageClient,
}

impl ProbeStrategy for PresignedUrlProbe {
    fn name(&self) -> &'static str {
        "PresignedUrl"
    }

    fn priority(&self) -> i32 {
        100
    }

    fn supports(&self, request: &ProbeRequest) -> bool {
        is_video_by_extension(&request.key)
    }

    fn probe(&self, request: &ProbeRequest) -> Result<ProbeOutcome, ProbeError> {
        let source = match &request.source {
            Some(s) if !s.trim().is_empty() => s.clone(),
            _ => self
                .storage
                .presign_get(&request.bucket, &request.key, 3600)
                .map_err(|e| ProbeError::ProbeFailed(format!("{} presign failed: {e}", self.name())))?,
        };
        let metadata = tc_ffprobe::probe(&self.ffprobe_path, &source)
            .map_err(|e| ProbeError::ProbeFailed(format!("{}: {e}", self.name())))?;
        Ok(ProbeOutcome {
            metadata,
            strategy: self.name(),
        })
    }
}

/// Fallback strategy: read first N MB from local file, then ffprobe temporary partial file.
/// This mirrors legacy "partial download probe" semantics.
pub struct PartialFileProbe {
    pub ffprobe_path: String,
    pub partial_size_mb: usize,
    pub storage: StorageClient,
}

impl ProbeStrategy for PartialFileProbe {
    fn name(&self) -> &'static str {
        "PartialDownload"
    }

    fn priority(&self) -> i32 {
        50
    }

    fn supports(&self, request: &ProbeRequest) -> bool {
        let is_partial_ext = supports_partial_extension(&request.key);
        if !is_partial_ext {
            return false;
        }
        match &request.source {
            Some(s) => {
                let p = Path::new(s);
                p.is_file() || s.starts_with("http://") || s.starts_with("https://")
            }
            None => true,
        }
    }

    fn probe(&self, request: &ProbeRequest) -> Result<ProbeOutcome, ProbeError> {
        let limit = self.partial_size_mb.saturating_mul(1024 * 1024).max(1024);
        let buf = match &request.source {
            Some(s) if s.starts_with("http://") || s.starts_with("https://") => self
                .storage
                .download_via_url(s, limit)
                .map_err(|e| ProbeError::Io(format!("http partial fetch failed: {e}")))?,
            Some(s) if Path::new(s).is_file() => {
                let mut input = File::open(s)
                    .map_err(|e| ProbeError::Io(format!("open partial source failed: {e}")))?;
                let mut data = vec![0u8; limit];
                let read_n = input
                    .read(&mut data)
                    .map_err(|e| ProbeError::Io(format!("read partial source failed: {e}")))?;
                data.truncate(read_n);
                data
            }
            _ => self
                .storage
                .download_partial(&request.bucket, &request.key, limit)
                .map_err(|e| ProbeError::Io(format!("minio partial download failed: {e}")))?,
        };

        let mut temp = std::env::temp_dir();
        let ts = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_nanos())
            .unwrap_or(0);
        temp.push(format!("bbmovie_probe_{}_{}.partial", std::process::id(), ts));

        let write_res = File::create(&temp)
            .and_then(|mut f| f.write_all(&buf))
            .map_err(|e| ProbeError::Io(format!("write partial temp failed: {e}")));
        if let Err(e) = write_res {
            let _ = std::fs::remove_file(&temp);
            return Err(e);
        }

        let result = tc_ffprobe::probe(&self.ffprobe_path, &temp.to_string_lossy())
            .map_err(|e| ProbeError::ProbeFailed(format!("{}: {e}", self.name())));
        let _ = std::fs::remove_file(&temp);
        let metadata = result?;
        Ok(ProbeOutcome {
            metadata,
            strategy: self.name(),
        })
    }
}

pub struct FastProbeService {
    strategies: Vec<Box<dyn ProbeStrategy>>,
}

impl FastProbeService {
    pub fn new(mut strategies: Vec<Box<dyn ProbeStrategy>>) -> Self {
        strategies.sort_by_key(|s| -s.priority());
        Self { strategies }
    }

    pub fn probe(&self, request: &ProbeRequest) -> Result<ProbeOutcome, ProbeError> {
        let mut last_error: Option<ProbeError> = None;
        for strategy in &self.strategies {
            if !strategy.supports(request) {
                continue;
            }
            match strategy.probe(request) {
                Ok(ok) => return Ok(ok),
                Err(e) => last_error = Some(e),
            }
        }
        Err(ProbeError::AllStrategiesFailed(match last_error {
            Some(e) => format!("all strategies failed for {}/{}: {e}", request.bucket, request.key),
            None => format!("no compatible strategy for {}/{}", request.bucket, request.key),
        }))
    }
}

pub fn default_fast_probe(ffprobe_path: impl Into<String>, partial_size_mb: usize) -> Result<FastProbeService> {
    let cfg = RuntimeConfig::from_env();
    let storage = StorageClient::new(cfg)?;
    let ffprobe_path = ffprobe_path.into();
    Ok(FastProbeService::new(vec![
        Box::new(PresignedUrlProbe {
            ffprobe_path: ffprobe_path.clone(),
            storage: storage.clone(),
        }),
        Box::new(PartialFileProbe {
            ffprobe_path,
            partial_size_mb: partial_size_mb.max(1),
            storage,
        }),
    ]))
}

fn is_video_by_extension(key: &str) -> bool {
    let k = key.to_ascii_lowercase();
    k.ends_with(".mp4")
        || k.ends_with(".mkv")
        || k.ends_with(".mov")
        || k.ends_with(".webm")
        || k.ends_with(".avi")
}

fn supports_partial_extension(key: &str) -> bool {
    let k = key.to_ascii_lowercase();
    k.ends_with(".mp4") || k.ends_with(".mov")
}
