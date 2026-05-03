use tc_vqs::score_with_stub_vmaf;
use transcode_contracts::dto::ValidationRequest;

fn request() -> ValidationRequest {
    ValidationRequest {
        upload_id: "u1".into(),
        playlist_path: "p.m3u8".into(),
        rendition_label: "1080p".into(),
        expected_width: 1920,
        expected_height: 1080,
    }
}

#[test]
fn dims_ok_gets_high_score() {
    let r = score_with_stub_vmaf(&request(), 1918, 1079);
    assert!(r.passed);
    assert_eq!(r.score, 92.0);
}

#[test]
fn dims_mismatch_gets_low_score() {
    let r = score_with_stub_vmaf(&request(), 1280, 720);
    assert!(!r.passed);
    assert_eq!(r.score, 35.0);
}