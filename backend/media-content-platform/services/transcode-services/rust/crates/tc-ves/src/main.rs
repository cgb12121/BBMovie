//! VES encoder worker placeholder — `encoding-queue` consumer (Temporal wiring TBD).

use transcode_contracts::dto::EncodeRequest;
use transcode_contracts::temporal;

fn main() {
    let sample = EncodeRequest {
        upload_id: "demo".into(),
        resolution: "1080p".into(),
        width: 1920,
        height: 1080,
        master_key: "00".into(),
        master_iv: "00".into(),
    };
    println!(
        "tc-ves (would poll {}): stub encode for {}",
        temporal::ENCODING, sample.resolution
    );
    let _ = serde_json::to_string(&sample).expect("serialize");
}
