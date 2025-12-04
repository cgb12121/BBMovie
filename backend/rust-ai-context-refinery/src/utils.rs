use axum::body::Bytes;
use axum::extract::Multipart;
use anyhow::{Result, anyhow};
use std::path::{Path, PathBuf};
use tokio::fs::File;
use tokio::io::AsyncWriteExt;
use uuid::Uuid;
use symphonia::core::audio::{SampleBuffer};
use symphonia::core::io::MediaSourceStream;
use symphonia::core::probe::Hint;

pub async fn save_temp_file(multipart: &mut Multipart) -> Result<(PathBuf, String)> {
    // Tale the first field
    while let Some(field) = multipart.next_field().await? {
        let filename: String = field.file_name()
            .unwrap_or("unknown")
            .to_string();
        let data: Bytes = field.bytes().await?;

        // Random file name (UUID) to prevent collisions
        let temp_name = format!("upload_{}_{}", Uuid::new_v4(), filename);
        let temp_path = std::env::temp_dir().join(&temp_name);

        // Async write to disk
        let mut file = File::create(&temp_path).await
            .map_err(|e| anyhow!("Failed to create temp file: {}", e))?;
        file.write_all(&data).await?;

        return Ok((temp_path, filename));
    }
    Err(anyhow!("No file found in multipart request"))
}

pub fn decode_audio(path: &Path) -> Result<Vec<f32>> {
    // Open file and create MediaSourceStream
    let src = std::fs::File::open(path)
        .map_err(|e| anyhow!("Failed to open file {:?}: {}", path, e))?;

    let mss = MediaSourceStream::new(Box::new(src), Default::default());

    // Probe format
    let mut hint = Hint::new();
    if let Some(ext) = path.extension().and_then(|e| e.to_str()) {
        hint.with_extension(ext);
    }

    let probed = symphonia::default::get_probe()
        .format(&hint, mss, &Default::default(), &Default::default())
        .map_err(|e| anyhow!("Failed to probe audio format: {}", e))?;

    let mut format = probed.format;
    let track = format.default_track().ok_or(anyhow!("No default track"))?;
    
    // Take Sample Rate of the original file
    let original_sample_rate = track.codec_params.sample_rate.unwrap_or(0);
    tracing::info!("🎵 File Sample Rate: {} Hz", original_sample_rate);

    let mut decoder = symphonia::default::get_codecs()
        .make(&track.codec_params, &Default::default())
        .map_err(|e| anyhow!("Failed to create decoder: {}", e))?;

    let track_id = track.id;
    let mut all_samples = Vec::new();

    // Decode Loop
    while let Ok(packet) = format.next_packet() {
        if packet.track_id() != track_id {
            continue;
        }
        match decoder.decode(&packet) {
            Ok(decoded) => {
                let spec = *decoded.spec();
                let duration = decoded.capacity();
                let mut sample_buf = SampleBuffer::<f32>::new(duration as u64, spec);
                sample_buf.copy_interleaved_ref(decoded);
                
                let samples = sample_buf.samples();
                let channels = spec.channels.count();
                
                // Convert to Mono (Average the channels or the left channel)
                for frame in samples.chunks(channels) {
                    let mono_sample = frame[0]; //  Take the left channel
                    all_samples.push(mono_sample);
                }
            }
            Err(_e) => break, 
        }
    }

    // RESAMPLE back to 16,000 HZ (MOST IMPORTANT) for whisper
    if original_sample_rate != 16000 {
        tracing::debug!("Resampling from {}Hz to 16000Hz...", original_sample_rate);
        let resampled = resample_linear(&all_samples, original_sample_rate, 16000);
        return Ok(resampled);
    }

    Ok(all_samples)
}

// Method to Simple Resample (Linear Interpolation), enough for Whisper
fn resample_linear(input: &[f32], old_rate: u32, new_rate: u32) -> Vec<f32> {
    if old_rate == new_rate {
        return input.to_vec();
    }

    let ratio = old_rate as f32 / new_rate as f32;
    let new_len = (input.len() as f32 / ratio).ceil() as usize;
    let mut output = Vec::with_capacity(new_len);

    for i in 0..new_len {
        let old_idx_float = i as f32 * ratio;
        let old_idx_floor = old_idx_float.floor() as usize;
        let old_idx_ceil = (old_idx_floor + 1).min(input.len() - 1);
        let weight = old_idx_float - old_idx_floor as f32;

        let sample = input[old_idx_floor] * (1.0 - weight) + input[old_idx_ceil] * weight;
        output.push(sample);
    }

    output
}