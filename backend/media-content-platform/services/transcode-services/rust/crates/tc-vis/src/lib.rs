//! Video inspection — fast probe strategies (VIS analogue).

use transcode_contracts::dto::VideoMetadata;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ProbeError {
    Io(String),
    ProbeFailed(String),
}

/// Outcome of a cheap probe before full analysis (`metadata` matches Java `MetadataDTO` JSON).
#[derive(Debug, Clone, PartialEq)]
pub struct ProbeOutcome {
    pub metadata: VideoMetadata,
    pub strategy: &'static str,
}

/// Pluggable probe strategy (presign URL vs ranged GET, etc.).
pub trait ProbeStrategy: Send + Sync {
    fn name(&self) -> &'static str;
    /// `source` is a filesystem path or URL, same as Java `FFprobe.probe(String)`.
    fn probe(&self, bucket: &str, source: &str) -> Result<ProbeOutcome, ProbeError>;
}

/// Real ffprobe (`-print_format json -show_streams -show_format`), same invocation style as Java bramp `FFprobe`.
pub struct FfprobeProbe {
    pub ffprobe_path: String,
}

impl ProbeStrategy for FfprobeProbe {
    fn name(&self) -> &'static str {
        "ffprobe"
    }

    fn probe(&self, _bucket: &str, source: &str) -> Result<ProbeOutcome, ProbeError> {
        let metadata = tc_ffprobe::probe(&self.ffprobe_path, source)
            .map_err(|e| ProbeError::ProbeFailed(e.to_string()))?;
        Ok(ProbeOutcome {
            metadata,
            strategy: self.name(),
        })
    }
}

/// Stub when no media is available (tests / demos).
pub struct StubProbe;

impl ProbeStrategy for StubProbe {
    fn name(&self) -> &'static str {
        "stub"
    }

    fn probe(&self, _bucket: &str, object_key: &str) -> Result<ProbeOutcome, ProbeError> {
        let height = if object_key.contains("4k") { 2160i32 } else { 1080i32 };
        let width = (height * 16 / 9).max(640);
        Ok(ProbeOutcome {
            metadata: VideoMetadata {
                width,
                height,
                duration_seconds: 1.0,
                codec: "h264".into(),
            },
            strategy: self.name(),
        })
    }
}
