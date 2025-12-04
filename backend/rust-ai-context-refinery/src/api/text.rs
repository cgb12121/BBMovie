use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;

use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

pub async fn handle_text_extract(mut multipart: Multipart) -> impl IntoResponse {
    let (temp_file, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("Processing Text File: {}", filename);
    
    let processing_path = temp_file.path().to_owned();
    let result = tokio::task::spawn_blocking(move || {
        services::text::extract_text_from_file(&processing_path)
    }).await;

    // Explicit close to catch/log errors (Issue 7)
    if let Err(e) = temp_file.close() {
         tracing::warn!("Failed to delete temp file for Text: {}", e);
    }

    match result {
        Ok(service_res) => match service_res {
            Ok(text) => response(
                StatusCode::OK,
                ApiResponse::success(
                    json!({ "filename": filename, "text": text }),
                    Some("Text extracted successfully".to_string())
                )
            ),
            Err(e) => response(
                StatusCode::INTERNAL_SERVER_ERROR,
                ApiResponse::failure(e.to_string(), None)
            )
        },
        Err(_) => response(
            StatusCode::INTERNAL_SERVER_ERROR,
            ApiResponse::failure("Text processing worker panicked".to_string(), None)
        )
    }
}
