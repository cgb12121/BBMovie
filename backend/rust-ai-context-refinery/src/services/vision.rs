use std::path::Path;
use anyhow::Result;
use serde_json::json;
use base64::{Engine as _, engine::general_purpose};

pub async fn describe_image(path: &Path) -> Result<String> {
    // Read the image file
    let image_bytes = tokio::fs::read(path).await?;
    let base64_image = general_purpose::STANDARD.encode(&image_bytes);

    // Call Ollama API
    let model = std::env::var("OLLAMA_VISION_MODEL")
        .unwrap_or_else(|_| "moondream".to_string());
    
    let client = reqwest::Client::new();
    let res = client.post("http://localhost:11434/api/generate")
        .json(&json!({
            "model": model,
            "prompt": "Describe this image in detail.",
            "stream": false,
            "images": [base64_image]
        }))
        .send()
        .await?;

    if !res.status().is_success() {
         return Err(anyhow::anyhow!("Ollama API error: {}", res.status()));
    }

    let body: serde_json::Value = res.json().await?;
    let description = body["response"].as_str()
        .unwrap_or("").to_string();
    
    Ok(description)
}