use std::path::Path;
use anyhow::{anyhow, Result, Context};
use lopdf::Document;

pub fn extract_text_from_pdf(path: &Path) -> Result<String> {
    // Load PDF Document
    let doc = Document::load(path)
        .with_context(|| format!("Failed to load PDF file: {:?}", path))?;

    let mut full_text = String::new();

    // Iterate through pages to extract text
    // NOTE: lopdf extract text might not be perfect 100%, but it's good enough for RAG
    for (page_num, _page_id) in doc.get_pages() {
        let text = doc.extract_text(&[page_num])
            .unwrap_or_default();
            
        if !text.trim().is_empty() {
            full_text.push_str(&text);
            full_text.push('\n'); // Add a newline for readability after each page
        }
    }

    if full_text.is_empty() {
        return Err(anyhow!("No text found in PDF (Scanned PDF? Use OCR endpoint instead)"));
    }

    Ok(full_text)
}


