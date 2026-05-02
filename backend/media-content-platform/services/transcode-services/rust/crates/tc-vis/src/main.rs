//! VIS — probe media and print **VideoMetadata** JSON (Java `MetadataDTO`: width, height, durationSeconds, codec).

use std::env;

use tc_lgs::ladder_for_metadata;
use tc_vis::{FfprobeProbe, ProbeStrategy};
use transcode_contracts::temporal;

fn main() {
    let ffprobe = env::var("FFPROBE_PATH").unwrap_or_else(|_| "ffprobe".into());
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        eprintln!(
            "usage: {} <path-or-url>\n\
             env: FFPROBE_PATH (default: ffprobe)\n\
             stdout: JSON VideoMetadata — same field names as Java MetadataDTO\n\
             stderr: ladder summary (would use Temporal task queue {})",
            args.get(0).map(String::as_str).unwrap_or("vis"),
            temporal::ANALYSIS
        );
        std::process::exit(2);
    }
    let input = &args[1];
    let probe = FfprobeProbe { ffprobe_path: ffprobe };
    let outcome = probe
        .probe("bbmovie-source", input)
        .expect("ffprobe probe failed");
    println!(
        "{}",
        serde_json::to_string_pretty(&outcome.metadata).expect("serialize VideoMetadata")
    );
    let ladder = ladder_for_metadata(&outcome.metadata);
    eprintln!(
        "# ladder (task queue {}): strategy={} rungs={}",
        temporal::ANALYSIS,
        outcome.strategy,
        ladder.len()
    );
    for r in ladder {
        eprintln!("#   {} {}x{} cost={}", r.label, r.width, r.height, r.relative_cost);
    }
}
