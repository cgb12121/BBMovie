use std::fs;
use std::path::Path;
use anyhow::{Result, Context};
use csv::ReaderBuilder;

pub fn extract_text_from_file(path: &Path) -> Result<String> {
    // Identify encoding? For now assume UTF-8.
    // We could use 'encoding_rs' if needed for legacy files, but UTF-8 is standard.
    let text: String = fs::read_to_string(path)
        .with_context(|| format!("Failed to read text file: {:?}", path))?;
    Ok(text)
}

pub fn summarize_csv(path: &Path) -> Result<String> {
    let mut rdr = ReaderBuilder::new().from_path(path)?;
    let headers = rdr.headers()?.clone();

    let mut markdown = String::new();
    markdown.push_str(&format!("CSV Summary (Total columns: {})\n", headers.len()));
    markdown.push_str(&format!("Columns: {:?}\n", headers));
    markdown.push_str("First 5 rows:\n");

    // Chỉ lấy 5 dòng đầu
    for (i, result) in rdr.records().take(5).enumerate() {
        let record = result?;
        markdown.push_str(&format!("{}. {:?}\n", i + 1, record));
    }

    // Đếm tổng dòng (nếu cần, nhưng coi chừng tốn IO đọc hết file)
    // Hoặc chỉ cần return đoạn sample kia là đủ để AI viết SQL/Query rồi.

    Ok(markdown)
}