use anyhow::{anyhow, Result};
use std::path::PathBuf;
use tracing::{info, error};
use crate::services;
use crate::dto::event::AssetEvent;
use tempfile::NamedTempFile;
use std::io::Write;

pub async fn process_asset(event: &AssetEvent) -> Result<String> {
    // 1. Download file to temp
    let temp_file = download_from_minio(event).await?;
    let path = temp_file.path();

    // 2. Identify and Process
    let extension = event.object_key.split('.').last().unwrap_or("").to_lowercase();
    
    let content = match extension.as_str() {
        "pdf" => services::pdf::extract_text_from_pdf(path)?,
        "png" | "jpg" | "jpeg" | "tiff" | "bmp" => services::ocr::extract_text_from_image(path).await?,
        "mp3" | "wav" | "m4a" | "ogg" => services::whisper::transcribe_audio(path).await?,
        "txt" | "md" | "json" | "csv" | "xml" => std::fs::read_to_string(path)?,
        _ => return Err(anyhow!("Unsupported file type: {}", extension)),
    };

    Ok(content)
}

async fn download_from_minio(event: &AssetEvent) -> Result<NamedTempFile> {
    let minio_url = std::env::var("MINIO_URL").unwrap_or_else(|_| "http://localhost:9000".to_string());
    let url = format!("{}/{}/{}", minio_url, event.bucket, event.object_key);
    
    info!("Downloading asset: {}", url);

    let response = reqwest::get(url).await?;
    if !response.status().is_success() {
        return Err(anyhow!("Failed to download from MinIO: {}", response.status()));
    }

    let bytes = response.bytes().await?;
    let mut temp = NamedTempFile::new()?;
    temp.write_all(&bytes)?;
    
    Ok(temp)
}
