use std::fs;
use std::path::Path;
use anyhow::{Result, Context};

pub fn extract_text_from_file(path: &Path) -> Result<String> {
    // Identify encoding? For now assume UTF-8.
    // We could use 'encoding_rs' if needed for legacy files, but UTF-8 is standard.
    let text = fs::read_to_string(path)
        .with_context(|| format!("Failed to read text file: {:?}", path))?;
    Ok(text)
}