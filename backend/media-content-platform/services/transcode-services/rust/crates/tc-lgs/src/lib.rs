//! Ladder generation service — preset rungs from source geometry (LGS analogue).

use transcode_contracts::dto::{SourceVideoSummary, VideoMetadata};

#[derive(Debug, Clone, PartialEq)]
pub struct LadderRung {
    pub label: &'static str,
    pub width: u32,
    pub height: u32,
    /// Unitless relative cost for scheduling hints.
    pub relative_cost: f64,
}

const PRESET: &[(&str, u32, u32, f64)] = &[
    ("2160p", 3840, 2160, 4.0),
    ("1440p", 2560, 1440, 2.8),
    ("1080p", 1920, 1080, 2.0),
    ("720p", 1280, 720, 1.2),
    ("480p", 854, 480, 0.7),
    ("360p", 640, 360, 0.45),
];

pub fn ladder_for_metadata(meta: &VideoMetadata) -> Vec<LadderRung> {
    let summary = SourceVideoSummary::from(meta);
    ladder_for_source(&summary)
}

/// Returns rungs at or below source height (inclusive of closest match), in descending resolution order.
pub fn ladder_for_source(summary: &SourceVideoSummary) -> Vec<LadderRung> {
    PRESET
        .iter()
        .filter(|(_, _, h, _)| *h <= summary.height)
        .map(|(label, w, h, c)| LadderRung {
            label,
            width: *w,
            height: *h,
            relative_cost: *c,
        })
        .collect()
}
