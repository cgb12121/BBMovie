use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;
use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

pub async fn handle_ocr(mut multipart: Multipart) -> impl IntoResponse {
    // 1. Save temp file
    let (temp_path, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("👁️ Running OCR on: {}", filename);

    // 2. Call Service OCR, run blocking because Tesseract is a heavy process
    let processing_path = temp_path.clone();
    let result = tokio::task::spawn_blocking(move || {
        services::ocr::run_ocr(&processing_path)
    }).await;

    // 3. Clean up temp file
    let _ = std::fs::remove_file(&temp_path);

    // 4. Error handling
    match result {
        Ok(service_result) => match service_result {
            Ok(text) => response(
                StatusCode::OK,
                ApiResponse::success(
                    json!({ "filename": filename, "text": text.trim() }),
                    Some("OCR extraction successful".to_string())
                )
            ),
            Err(e) => {
                tracing::error!("OCR Error: {:?}", e);
                response(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    ApiResponse::failure(e.to_string(), None)
                )
            }
        },
        Err(_join_err) => response(
            StatusCode::INTERNAL_SERVER_ERROR,
            ApiResponse::failure("OCR Worker panicked".to_string(), None)
        ),
    }
}