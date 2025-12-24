//! Domain services for AI context refinery.
//!
//! Split by concern:
//! - `pdf`     → text extraction from PDF
//! - `ocr`     → OCR extraction from images
//! - `whisper` → audio transcription abstraction
//! - `text`    → generic text extraction
//! - `vision`  → image analysis via Ollama

pub mod pdf;
pub mod ocr;
pub mod whisper;
pub mod text;
pub mod vision;
pub mod redis;
pub mod nats;


