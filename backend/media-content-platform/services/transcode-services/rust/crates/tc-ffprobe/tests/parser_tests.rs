use tc_ffprobe::{parse_probe_json, probe};

#[test]
fn parses_sample_like_ffprobe_json() {
    let json = r#"{
        "streams": [
            {
                "index": 0,
                "codec_name": "h264",
                "codec_type": "video",
                "width": 1920,
                "height": 1080,
                "duration": "125.440000"
            }
        ],
        "format": {
            "duration": "125.440000"
        }
    }"#;
    let m = parse_probe_json(json).expect("parse");
    assert_eq!(m.width, 1920);
    assert_eq!(m.height, 1080);
    assert!((m.duration_seconds - 125.44).abs() < 0.01);
    assert_eq!(m.codec, "h264");
}

#[test]
fn parses_string_dimensions() {
    let json = r#"{
        "streams": [{"codec_type": "video", "codec_name": "hevc", "width": "3840", "height": "2160"}],
        "format": {"duration": "60.0"}
    }"#;
    let m = parse_probe_json(json).unwrap();
    assert_eq!(m.width, 3840);
    assert_eq!(m.height, 2160);
}

#[test]
#[ignore]
fn probe_real_media_when_env_set() {
    let path = std::env::var("BBMOVIE_FFPROBE_MEDIA").expect("BBMOVIE_FFPROBE_MEDIA");
    let exe = std::env::var("FFPROBE_PATH").unwrap_or_else(|_| "ffprobe".into());
    let m = probe(&exe, &path).expect("ffprobe");
    assert!(m.width > 0 && m.height > 0);
}