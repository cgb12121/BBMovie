//! Video validation — dimension gate (VVS analogue).

use tc_ffprobe::FfprobeError;
use transcode_contracts::dto::{QualityReport, ValidationRequest};

const DIM_TOLERANCE: i32 = 8;

pub fn validate_playlist(
    ffprobe_exe: &str,
    playlist_path: &str,
    req: &ValidationRequest,
) -> Result<QualityReport, FfprobeError> {
    let meta = tc_ffprobe::probe(ffprobe_exe, playlist_path)?;
    Ok(validate_dimensions(req, meta.width, meta.height))
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
        detail: "vvs_ffprobe_dimensions".into(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn within_tolerance_passes() {
        let req = ValidationRequest {
            upload_id: "u1".into(),
            playlist_path: "p.m3u8".into(),
            rendition_label: "1080p".into(),
            expected_width: 1920,
            expected_height: 1080,
        };
        let r = validate_dimensions(&req, 1924, 1076);
        assert!(r.passed);
    }
}
