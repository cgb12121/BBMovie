use std::path::Path;
use anyhow::{Result, anyhow};
use serde_json::json;
use base64::{Engine as _, engine::general_purpose};
use reqwest::{Client, Response};

pub async fn describe_image(path: &Path) -> Result<String> {
    // Read the image file
    let image_bytes: Vec<u8> = tokio::fs::read(path).await?;
    let base64_image: String = general_purpose::STANDARD.encode(&image_bytes);

    // Call Ollama API
    let model: String = std::env::var("OLLAMA_VISION_MODEL")
        .unwrap_or_else(|_| "moondream".to_string());
    let base_url: String = std::env::var("OLLAMA_URL")
        .unwrap_or_else(|_| "http://localhost:11434".to_string());
    
    let client: Client = Client::new();
    let res: Response = client.post(format!("{}/api/generate", base_url))
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
    
    // Don't use unwrap_or(""), return error if the response is missing/invalid
    let description: String = body["response"].as_str()
        .ok_or_else(|| anyhow!("Invalid response from Ollama: 'response' field missing or not a string"))?
        .to_string();
    
    Ok(description)
}
