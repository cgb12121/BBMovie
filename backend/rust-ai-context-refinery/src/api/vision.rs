use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;

use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

pub async fn handle_image_analysis(mut multipart: Multipart) -> impl IntoResponse {
    let (temp_path, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("Analyzing Image: {}", filename);
    let result = services::vision::describe_image(&temp_path).await;

    let _ = std::fs::remove_file(&temp_path);

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
