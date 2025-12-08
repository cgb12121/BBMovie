//! Core library for the `ai-refinery` service.
//!
//! This crate is split into simple layers:
//! - `api`: HTTP layer (Axum route handlers)
//! - `services`: domain/business logic (PDF, OCR, Whisper)
//! - `infra`: infrastructure (Service Discovery, etc.)
//! - `security`: Authentication and Authorization
//! - `utils`: shared helpers (file I/O, multipart handling)
//!
//! The binary (`main.rs`) should stay as thin as possible.

pub mod api;
pub mod services;
pub mod infra;
pub mod utils;
pub mod dto;