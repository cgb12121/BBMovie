use std::path::Path;
use anyhow::{Result, anyhow};
use serde_json::json;
use base64::{Engine as _, engine::general_purpose};

pub async fn describe_image(path: &Path) -> Result<String> {
    // Read the image file
    let image_bytes = tokio::fs::read(path).await?;
    let base64_image = general_purpose::STANDARD.encode(&image_bytes);

    // Call Ollama API
    let model = std::env::var("OLLAMA_VISION_MODEL")
        .unwrap_or_else(|_| "moondream".to_string());
    let base_url = std::env::var("OLLAMA_URL")
        .unwrap_or_else(|_| "http://localhost:11434".to_string());
    
    let client = reqwest::Client::new();
    let res = client.post(format!("{}/api/generate", base_url))
        .json(&json!({
            "model": model,
            "prompt": "Describe this image in detail.",
            "stream": false,
            "images": [base64_image]
        }))
        .send()
        .await?;

    if !res.status().is_success() {
         return Err(anyhow!("Ollama API error: {}", res.status()));
    }

    let body: serde_json::Value = res.json().await?;
    
    // Issue 8: Don't use unwrap_or(""), return error if response is missing/invalid
    let description = body["response"].as_str()
        .ok_or_else(|| anyhow!("Invalid response from Ollama: 'response' field missing or not a string"))?
        .to_string();
    
    Ok(description)
}
