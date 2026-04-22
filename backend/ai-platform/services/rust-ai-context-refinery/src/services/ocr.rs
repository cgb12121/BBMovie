use std::path::Path;

use anyhow::Result;
use anyhow::anyhow;
use rusty_tesseract::{Args, Image, TessError};

/// OCR Service
pub fn run_ocr(path: &Path) -> Result<String> {
    // Load image
    let img: Image = Image::from_path(path)
        .map_err(|e: TessError| anyhow!("Failed to load image for OCR: {}", e))?;

    // Setting Tesseract options/parameters
    let args = Args {
        // Auto-detect language both Eng+Vie (if vie package is installed)
        lang: "eng+vie".to_string(), 
        
        // Config layout (3 = Fully automatic page segmentation, but no OSD)
        psm: Some(3), 
        
        // Output config (Default is good enough for most cases)
        dpi: Some(300), // Suggest DPI if the image does not have metadata
        ..Default::default()
    };

    // Run tesseract cmd
    // NOTE: the code will fail if Tesseract is not installed or not in PATH
    let output: String = rusty_tesseract::image_to_string(&img, &args)
        .map_err(|e: TessError| anyhow!("Tesseract execution failed: {}. \\n\
            👉 Hint: Check if Tesseract is installed and in System PATH.", e
        ))?;

    Ok(output)
}