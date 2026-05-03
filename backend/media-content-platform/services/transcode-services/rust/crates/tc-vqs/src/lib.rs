//! Video quality scoring — VMAF path TBD (VQS analogue).

use std::fs::{self, File};
use std::io::Write;
use std::path::PathBuf;
use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};

use tc_ffprobe::FfprobeError;
use tc_runtime::config::RuntimeConfig;
use tc_runtime::storage::StorageClient;
use transcode_contracts::dto::{QualityReport, ValidationRequest};

const DIM_TOLERANCE: i32 = 8;

pub fn score_playlist(
    ffprobe_exe: &str,
    playlist_path: &str,
    req: &ValidationRequest
) -> QualityReport {
    match tc_ffprobe::probe(ffprobe_exe, playlist_path) {
        Ok(meta) => score_with_stub_vmaf(req, meta.width, meta.height),
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

pub fn score_from_storage_and_vmaf(
    cfg: &RuntimeConfig,
    storage: &StorageClient,
    req: &ValidationRequest,
) -> QualityReport {
    let workdir = temp_workdir(&req.upload_id);
    if let Err(e) = fs::create_dir_all(&workdir) {
        return fail(req, format!("create_workdir_failed: {e}"));
    }

    let target_playlist = workdir.join("target_playlist.m3u8");
    let reference_source = workdir.join("reference_source.mp4");
    let vmaf_json = workdir.join("vmaf.json");

    let target_bytes = match storage.download_object(&cfg.hls_bucket, &req.playlist_path) {
        Ok(v) => v,
        Err(e) => return fail(req, format!("download_target_failed: {e}")),
    };
    if let Err(e) = File::create(&target_playlist).and_then(|mut f| f.write_all(&target_bytes)) {
        return fail(req, format!("write_target_failed: {e}"));
    }

    let ref_key = cfg.reference_key_for_upload(&req.upload_id);
    let ref_bytes = match storage.download_object(&cfg.source_bucket, &ref_key) {
        Ok(v) => v,
        Err(e) => return fail(req, format!("download_reference_failed: {e}")),
    };
    if let Err(e) = File::create(&reference_source).and_then(|mut f| f.write_all(&ref_bytes)) {
        return fail(req, format!("write_reference_failed: {e}"));
    }

    let base = score_playlist(&cfg.ffprobe_path, target_playlist.to_string_lossy().as_ref(), req);
    if !base.passed {
        let _ = fs::remove_dir_all(&workdir);
        return base;
    }

    let filter = format!(
        "libvmaf=log_fmt=json:log_path={}",
        vmaf_json.to_string_lossy()
    );
    let output = Command::new(&cfg.ffmpeg_path)
        .args(["-y", "-i"])
        .arg(&reference_source)
        .args(["-i"])
        .arg(&target_playlist)
        .args(["-lavfi"])
        .arg(filter)
        .args(["-f", "null", "-"])
        .output();

    let ffmpeg_out = match output {
        Ok(o) if o.status.success() => o,
        Ok(o) => {
            return fail(
                req,
                format!(
                    "ffmpeg_vmaf_failed code={} stderr={}",
                    o.status.code().unwrap_or(-1),
                    String::from_utf8_lossy(&o.stderr)
                ),
            )
        }
        Err(e) => return fail(req, format!("spawn_ffmpeg_failed: {e}")),
    };

    let vmaf_score = match parse_vmaf_json(&vmaf_json) {
        Ok(s) => s,
        Err(e) => {
            return fail(
                req,
                format!(
                    "parse_vmaf_failed: {e}; stderr={}",
                    String::from_utf8_lossy(&ffmpeg_out.stderr)
                ),
            )
        }
    };

    let _ = fs::remove_dir_all(&workdir);
    QualityReport {
        rendition_label: req.rendition_label.clone(),
        passed: vmaf_score >= 80.0,
        score: vmaf_score,
        detail: format!(
            "vqs_libvmaf expected={}x{} vmaf={:.3}",
            req.expected_width, req.expected_height, vmaf_score
        ),
    }
}

pub fn score_with_stub_vmaf(
    req: &ValidationRequest,
    actual_width: i32,
    actual_height: i32,
) -> QualityReport {
    let dw = (actual_width - req.expected_width).abs();
    let dh = (actual_height - req.expected_height).abs();
    let dims_ok = dw <= DIM_TOLERANCE && dh <= DIM_TOLERANCE;
    let score = if dims_ok { 92.0 } else { 35.0 };
    QualityReport {
        rendition_label: req.rendition_label.clone(),
        passed: dims_ok,
        score,
        detail: format!(
            "vqs_ffprobe_dimensions_vmaf_stub expected={}x{} actual={}x{} tolerance={}",
            req.expected_width, req.expected_height, actual_width, actual_height, DIM_TOLERANCE
        ),
    }
}

fn parse_vmaf_json(path: &PathBuf) -> Result<f64, String> {
    let text = fs::read_to_string(path).map_err(|e| e.to_string())?;
    let value: serde_json::Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
    let pooled = value
        .get("pooled_metrics")
        .and_then(|pm| pm.get("vmaf"))
        .and_then(|v| v.get("mean"))
        .and_then(|m| m.as_f64());
    pooled.ok_or_else(|| "missing pooled_metrics.vmaf.mean".to_string())
}

fn fail(req: &ValidationRequest, detail: String) -> QualityReport {
    QualityReport {
        rendition_label: req.rendition_label.clone(),
        passed: false,
        score: 0.0,
        detail,
    }
}

fn temp_workdir(upload_id: &str) -> PathBuf {
    let mut p = std::env::temp_dir();
    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    p.push(format!("vqs_{}_{}", upload_id, ts));
    p
}
