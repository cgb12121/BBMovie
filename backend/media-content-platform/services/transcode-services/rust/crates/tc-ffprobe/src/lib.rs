//! Run `ffprobe -print_format json -show_format -show_streams` and map the first video stream
//! to [`transcode_contracts::dto::VideoMetadata`], matching Java `MetadataDTO` / `LgsSourceVideoMetadata`
//! / `FFmpegVideoMetadata` (width, height, duration, codec).

use std::path::Path;
use std::process::Command;

use serde::Deserialize;
use serde_json::Value;
use transcode_contracts::dto::VideoMetadata;

const DEFAULT_ARGS: &[&str] = &[
    "-v",
    "error",
    "-hide_banner",
    "-print_format",
    "json",
    "-show_format",
    "-show_streams",
];

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum FfprobeError {
    Io(String),
    NonZeroExit(i32, String),
    Parse(String),
    NoVideoStream,
}

impl std::fmt::Display for FfprobeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            FfprobeError::Io(s) => write!(f, "io: {s}"),
            FfprobeError::NonZeroExit(code, stderr) => {
                write!(f, "ffprobe exited {code}: {stderr}")
            }
            FfprobeError::Parse(s) => write!(f, "parse: {s}"),
            FfprobeError::NoVideoStream => write!(f, "no video stream in ffprobe output"),
        }
    }
}

impl std::error::Error for FfprobeError {}

/// Probe a local path or URL (same as Java `FFprobe.probe(String)`).
pub fn probe(ffprobe_exe: impl AsRef<Path>, input: &str) -> Result<VideoMetadata, FfprobeError> {
    let exe = ffprobe_exe.as_ref();
    let output = Command::new(exe)
        .args(DEFAULT_ARGS.iter().copied())
        .arg(input)
        .output()
        .map_err(|e| FfprobeError::Io(format!("failed to spawn {}: {e}", exe.display())))?;

    if !output.status.success() {
        let code = output.status.code().unwrap_or(-1);
        let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
        return Err(FfprobeError::NonZeroExit(code, stderr));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    parse_probe_json(&stdout)
}

/// Parse ffprobe JSON stdout (for tests and callers that already ran ffprobe).
pub fn parse_probe_json(json: &str) -> Result<VideoMetadata, FfprobeError> {
    let root: Root = serde_json::from_str(json).map_err(|e| FfprobeError::Parse(e.to_string()))?;

    let stream = root
        .streams
        .iter()
        .find(|s| {
            s.codec_type
                .as_deref()
                .map(|t| t.eq_ignore_ascii_case("video"))
                .unwrap_or(false)
        })
        .ok_or(FfprobeError::NoVideoStream)?;

    let width = parse_dimension_field("width", stream.width.as_ref())?;
    let height = parse_dimension_field("height", stream.height.as_ref())?;

    let codec = stream
        .codec_name
        .clone()
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| "unknown".into());

    let stream_dur = stream.duration.as_deref().and_then(parse_f64_loose);
    let format_dur = root
        .format
        .as_ref()
        .and_then(|f| f.duration.as_deref())
        .and_then(parse_f64_loose);
    let duration = stream_dur
        .filter(|d| *d > 0.0)
        .or(format_dur.filter(|d| *d > 0.0))
        .unwrap_or(0.0);

    Ok(VideoMetadata {
        width,
        height,
        duration_seconds: duration,
        codec,
    })
}

#[derive(Debug, Deserialize)]
struct Root {
    streams: Vec<Stream>,
    format: Option<Format>,
}

#[derive(Debug, Deserialize)]
struct Stream {
    codec_type: Option<String>,
    codec_name: Option<String>,
    width: Option<Value>,
    height: Option<Value>,
    duration: Option<String>,
}

#[derive(Debug, Deserialize)]
struct Format {
    duration: Option<String>,
}

fn parse_dimension_field(name: &'static str, v: Option<&Value>) -> Result<i32, FfprobeError> {
    let v = v.ok_or_else(|| FfprobeError::Parse(format!("video stream missing {name}")))?;
    match v {
        Value::Number(n) => {
            if let Some(i) = n.as_i64() {
                return i32::try_from(i).map_err(|_| {
                    FfprobeError::Parse(format!(
                        "{name} value {i} is outside representable i32 range"
                    ))
                });
            }
            let f = n
                .as_f64()
                .ok_or_else(|| FfprobeError::Parse(format!("{name} number not representable")))?;
            if !f.is_finite() {
                return Err(FfprobeError::Parse(format!("{name} is not finite")));
            }
            let i = f as i64;
            i32::try_from(i).map_err(|_| {
                FfprobeError::Parse(format!(
                    "{name} value {} is outside representable i32 range",
                    f
                ))
            })
        }
        Value::String(s) => {
            let trimmed = s.trim();
            if let Ok(parsed) = trimmed.parse::<i64>() {
                return i32::try_from(parsed).map_err(|_| {
                    FfprobeError::Parse(format!(
                        "{name} integer {parsed} is outside representable i32 range"
                    ))
                });
            }
            let f: f64 = trimmed
                .parse()
                .map_err(|_| FfprobeError::Parse(format!("{name} invalid string: {trimmed:?}")))?;
            if !f.is_finite() {
                return Err(FfprobeError::Parse(format!("{name} is not finite")));
            }
            let i = f as i64;
            i32::try_from(i).map_err(|_| {
                FfprobeError::Parse(format!(
                    "{name} value {} is outside representable i32 range",
                    f
                ))
            })
        }
        _ => Err(FfprobeError::Parse(format!(
            "{name} has unsupported JSON type"
        ))),
    }
}

fn parse_f64_loose(s: &str) -> Option<f64> {
    let s = s.trim();
    if s.is_empty() {
        return None;
    }
    s.parse::<f64>().ok()
}
