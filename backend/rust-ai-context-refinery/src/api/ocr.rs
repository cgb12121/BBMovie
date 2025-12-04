use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;
use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

pub async fn handle_ocr(mut multipart: Multipart) -> impl IntoResponse {
    let (temp_file, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("👁️ Running OCR on: {}", filename);

    let processing_path = temp_file.path().to_owned();
    let result = tokio::task::spawn_blocking(move || {
        services::ocr::run_ocr(&processing_path)
    }).await;

    // Explicit close to catch/log errors (Issue 7)
    if let Err(e) = temp_file.close() {
         tracing::warn!("Failed to delete temp file for OCR: {}", e);
    }

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
        Err(join_err) => {
            tracing::error!("OCR worker thread panicked: {:?}", join_err);
            let msg = if join_err.is_panic() {
                "OCR worker thread panicked. Check logs for details."
            } else {
                "OCR worker thread was cancelled."
            };
            response(
                StatusCode::INTERNAL_SERVER_ERROR,
                ApiResponse::failure(msg.to_string(), None)
            )
        },
    }
}
