//! Video quality scoring — VMAF path TBD (VQS analogue).

use tc_ffprobe::FfprobeError;
use transcode_contracts::dto::{QualityReport, ValidationRequest};

const DIM_TOLERANCE: i32 = 8;

pub fn score_playlist(
    ffprobe_exe: &str,
    playlist_path: &str,
    req: &ValidationRequest,
) -> Result<QualityReport, FfprobeError> {
    let meta = tc_ffprobe::probe(ffprobe_exe, playlist_path)?;
    Ok(score_with_stub_vmaf(req, meta.width, meta.height))
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
        detail: "vqs_ffprobe_dimensions_vmaf_stub".into(),
    }
}
