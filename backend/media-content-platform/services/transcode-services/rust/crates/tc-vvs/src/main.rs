//! VVS — ffprobe HLS playlist, then print **QualityReport** JSON (Java `QualityReportDTO` field names).

use std::env;
use std::fs;

use tc_vvs::validate_playlist;
use transcode_contracts::dto::ValidationRequest;
use transcode_contracts::temporal;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() != 3 {
        eprintln!(
            "usage: {} <playlist.m3u8> <validation-request.json>\n\
             env: FFPROBE_PATH (default: ffprobe)\n\
             JSON fields: uploadId, playlistPath, renditionLabel, expectedWidth, expectedHeight (camelCase)\n\
             stdout: QualityReport — task queue {}",
            args.first().map(String::as_str).unwrap_or("vvs"),
            temporal::QUALITY
        );
        std::process::exit(2);
    }
    let ffprobe = env::var("FFPROBE_PATH").unwrap_or_else(|_| "ffprobe".into());
    let playlist = &args[1];
    let json = fs::read_to_string(&args[2]).expect("read validation json");
    let req: ValidationRequest = serde_json::from_str(&json).expect("parse ValidationRequest");
    let report = validate_playlist(&ffprobe, playlist, &req).expect("ffprobe / validate");
    println!(
        "{}",
        serde_json::to_string_pretty(&report).expect("serialize QualityReport")
    );
}
