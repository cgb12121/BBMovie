use tc_lgs::ladder_for_source;
use transcode_contracts::dto::SourceVideoSummary;

#[test]
fn caps_at_source_height() {
    let s = SourceVideoSummary {
        width: 1920,
        height: 1080,
        duration_sec: Some(120.0),
    };
    let rungs = ladder_for_source(&s);
    assert!(rungs.iter().all(|r| r.height <= 1080));
    assert!(rungs.iter().any(|r| r.label == "1080p"));
    assert!(!rungs.iter().any(|r| r.label == "1440p"));
}