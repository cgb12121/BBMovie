//! HTTP handlers grouped by concern:
//! - `pdf`   → extract text from PDF
//! - `ocr`   → OCR for images
//! - `audio` → audio transcription via Whisper
//! - `text`  → generic text extraction
//! - `vision`→ image analysis

pub mod pdf;
pub mod ocr;
pub mod audio;
pub mod text;
pub mod vision;

// Re-export the main entrypoint so main.rs can stay simple:
pub use pdf::handle_pdf_extract;
pub use ocr::handle_ocr;
pub use audio::handle_transcribe;
pub use text::handle_text_extract;
pub use vision::handle_image_analysis;
pub use crate::dto::response::ApiResponse;


