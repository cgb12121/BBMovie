use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;

use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

pub async fn handle_image_analysis(mut multipart: Multipart) -> impl IntoResponse {
    let (temp_file, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("Analyzing Image: {}", filename);
    
    // Vision is async (HTTP call to Ollama), so no spawn_blocking needed for the service call itself,
    // but the file IO inside describe_image is async.
    let result = services::vision::describe_image(temp_file.path()).await;

    // Explicit close to catch/log errors (Issue 7)
    if let Err(e) = temp_file.close() {
         tracing::warn!("Failed to delete temp file for Vision: {}", e);
    }

    match result {
        Ok(description) => response(
            StatusCode::OK,
            ApiResponse::success(
                json!({ "filename": filename, "description": description }),
                Some("Image analysis successful".to_string())
            )
        ),
        Err(e) => response(
            StatusCode::INTERNAL_SERVER_ERROR,
            ApiResponse::failure(e.to_string(), None)
        ),
    }
}