use std::time::{SystemTime, UNIX_EPOCH};

use tc_vis::{default_fast_probe, ProbeRequest};

#[test]
fn fast_probe_reports_no_strategy_for_non_video_extension() {
    let service = default_fast_probe("ffprobe", 1).expect("service init");
    let req = ProbeRequest {
        bucket: "b".into(),
        key: "notes.txt".into(),
        source: Some("notes.txt".into()),
    };
    let err = service.probe(&req).expect_err("must fail");
    let msg = format!("{err:?}");
    assert!(msg.contains("no compatible strategy") || msg.contains("all strategies failed"));
}

#[test]
#[ignore]
fn partial_probe_path_can_run_with_local_media() {
    let path = std::env::var("BBMOVIE_FFPROBE_MEDIA").expect("BBMOVIE_FFPROBE_MEDIA");
    let ext = if path.to_ascii_lowercase().ends_with(".mov") { "movie.mov" } else { "movie.mp4" };
    let service = default_fast_probe(
        std::env::var("FFPROBE_PATH").unwrap_or_else(|_| "ffprobe".into()),
        2,
    )
    .expect("service init");
    let req = ProbeRequest {
        bucket: "b".into(),
        key: ext.into(),
        source: Some(path),
    };
    let out = service.probe(&req).expect("probe");
    assert!(out.metadata.width > 0 && out.metadata.height > 0);
}

#[test]
fn infer_unique_temp_names_sanity() {
    let a = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos();
    let b = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos();
    assert!(b >= a);
}