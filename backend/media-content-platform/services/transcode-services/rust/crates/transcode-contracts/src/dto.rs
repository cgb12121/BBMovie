use serde::{Deserialize, Serialize};

/// Matches Java `MetadataDTO` / `LgsSourceVideoMetadata` / `FFmpegVideoMetadata` JSON field names.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VideoMetadata {
    pub width: i32,
    pub height: i32,
    pub duration_seconds: f64,
    pub codec: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EncodeRequest {
    pub upload_id: String,
    pub resolution: String,
    pub width: u32,
    pub height: u32,
    pub master_key: String,
    pub master_iv: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RungResult {
    pub resolution: String,
    pub playlist_path: String,
    pub success: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ValidationRequest {
    pub upload_id: String,
    pub playlist_path: String,
    pub rendition_label: String,
    pub expected_width: i32,
    pub expected_height: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QualityReport {
    pub rendition_label: String,
    pub passed: bool,
    pub score: f64,
    pub detail: String,
}

/// Ladder-only subset (no codec). Still used where codec is not needed.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SourceVideoSummary {
    pub width: u32,
    pub height: u32,
    pub duration_sec: Option<f64>,
}
impl From<&VideoMetadata> for SourceVideoSummary {
    fn from(m: &VideoMetadata) -> Self {
        SourceVideoSummary {
            width: m.width.max(0) as u32,
            height: m.height.max(0) as u32,
            duration_sec: (m.duration_seconds > 0.0).then_some(m.duration_seconds),
        }
    }
}
