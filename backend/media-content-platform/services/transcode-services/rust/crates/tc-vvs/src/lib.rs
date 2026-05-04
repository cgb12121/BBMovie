//! Video validation — dimension gate (VVS analogue).

use std::fs::File;
use std::hash::{Hash, Hasher};
use std::io::Write;
use std::time::{SystemTime, UNIX_EPOCH};

use tc_ffprobe::FfprobeError;
use tc_runtime::config::RuntimeConfig;
use tc_runtime::storage::StorageClient;
use transcode_contracts::dto::{QualityReport, ValidationRequest};

const DIM_TOLERANCE: i32 = 8;

pub fn validate_from_storage(
    cfg: &RuntimeConfig,
    storage: &StorageClient,
    req: &ValidationRequest
) -> QualityReport {
    match storage.download_object(&cfg.hls_bucket, &req.playlist_path) {
        Ok(bytes) => {
            let temp_path = temp_playlist_path(&req.upload_id);
            let write_result = File::create(&temp_path).and_then(|mut f| f.write_all(&bytes));
            if let Err(e) = write_result {
                return QualityReport {
                    rendition_label: req.rendition_label.clone(),
                    passed: false,
                    score: 0.0,
                    detail: format!("write_temp_playlist_failed: {e}"),
                };
            }
            let report = validate_playlist(&cfg.ffprobe_path, temp_path.to_string_lossy().as_ref(), req);
            let _ = std::fs::remove_file(temp_path);
            report
        }
        Err(e) => QualityReport {
            rendition_label: req.rendition_label.clone(),
            passed: false,
            score: 0.0,
            detail: format!("download_playlist_failed: {e}"),
        },
    }
}

pub fn validate_playlist(
    ffprobe_exe: &str,
    playlist_path: &str,
    req: &ValidationRequest
) -> QualityReport {
    match tc_ffprobe::probe(ffprobe_exe, playlist_path) {
        Ok(meta) => validate_dimensions(req, meta.width, meta.height),
        Err(FfprobeError::NoVideoStream) => QualityReport {
            rendition_label: req.rendition_label.clone(),
            passed: false,
            score: 0.0,
            detail: "no_video_stream".into(),
        },
        Err(e) => QualityReport {
            rendition_label: req.rendition_label.clone(),
            passed: false,
            score: 0.0,
            detail: e.to_string(),
        },
    }
}

pub fn validate_dimensions(
    req: &ValidationRequest,
    actual_width: i32,
    actual_height: i32,
) -> QualityReport {
    let dw = (actual_width - req.expected_width).abs();
    let dh = (actual_height - req.expected_height).abs();
    let passed = dw <= DIM_TOLERANCE && dh <= DIM_TOLERANCE;
    let score = if passed { 90.0 } else { 40.0 };
    QualityReport {
        rendition_label: req.rendition_label.clone(),
        passed,
        score,
        detail: format!(
            "vvs_ffprobe_dimensions expected={}x{} actual={}x{} tolerance={}",
            req.expected_width, req.expected_height, actual_width, actual_height, DIM_TOLERANCE
        ),
    }
}

fn temp_playlist_path(upload_id: &str) -> std::path::PathBuf {
    let mut p = std::env::temp_dir();
    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let safe_upload_id = sanitize_upload_id(upload_id);
    p.push(format!("vvs_{}_{}_playlist.m3u8", safe_upload_id, ts));
    p
}

fn sanitize_upload_id(upload_id: &str) -> String {
    let mut out = String::with_capacity(upload_id.len().min(48));
    for ch in upload_id.chars() {
        if ch.is_ascii_alphanumeric() || ch == '-' || ch == '_' {
            out.push(ch);
        } else if ch.is_ascii_whitespace() {
            out.push('_');
        }
    }
    if out.len() > 48 {
        out.truncate(48);
    }
    if out.is_empty() {
        return fallback_upload_id_token(upload_id);
    }
    out
}

fn fallback_upload_id_token(upload_id: &str) -> String {
    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    upload_id.hash(&mut hasher);
    format!("upload_{:08x}", (hasher.finish() & 0xffff_ffff) as u32)
}
