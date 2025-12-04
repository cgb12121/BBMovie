use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;

use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

/// Handle PDF upload & extraction.
pub async fn handle_pdf_extract(mut multipart: Multipart) -> impl IntoResponse {
    // 1. Save uploaded file to disk
    let (temp_path, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    // 2. Process via service layer
    tracing::info!("Processing PDF: {}", filename);
    let result = services::pdf::extract_text_from_pdf(&temp_path);

    // 3. Cleanup temp file
    let _ = std::fs::remove_file(&temp_path);

    // 4. Build response
    match result {
        Ok(text) => response(
            StatusCode::OK,
            ApiResponse::success(
                json!({ "filename": filename, "text": text }),
                Some("PDF extracted successfully".to_string())
            )
        ),
        Err(e) => response(
            StatusCode::INTERNAL_SERVER_ERROR,
            ApiResponse::failure(e.to_string(), None)
        ),
    }
}