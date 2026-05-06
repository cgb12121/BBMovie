use tc_vvs::validate_dimensions;
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
fn within_tolerance_passes() {
    let r = validate_dimensions(&request(), 1924, 1076);
    assert!(r.passed);
    assert_eq!(r.score, 90.0);
}

#[test]
fn outside_tolerance_fails() {
    let r = validate_dimensions(&request(), 1800, 1000);
    assert!(!r.passed);
    assert_eq!(r.score, 40.0);
}