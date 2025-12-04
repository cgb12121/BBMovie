use axum::{
    extract::Multipart,
    http::StatusCode,
    response::IntoResponse,
};
use serde_json::json;
use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};

/// Handle audio upload and transcription via Whisper.
pub async fn handle_transcribe(mut multipart: Multipart) -> impl IntoResponse {
    let (temp_path, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("Transcribing Audio: {}", filename);
    let processing_path = temp_path.clone();

    // Run Whisper in a blocking thread
    let task_result = tokio::task::spawn_blocking(move || {
        services::whisper::run_whisper(&processing_path)
    }).await;

    let _ = std::fs::remove_file(&temp_path);

    match task_result {
        Ok(service_result) => match service_result {
            Ok(text) => response(
                StatusCode::OK, 
                ApiResponse::success(
                    json!({ "filename": filename, "text": text }),
                    Some("Audio transcribed successfully".to_string())
                )
            ),
            Err(e) => {
                tracing::error!("Whisper Logic Error: {:?}", e);
                response(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    ApiResponse::failure(e.to_string(), None)
                )
            }
        },
        Err(join_err) => {
            tracing::error!("THREAD PANIC: {:?}", join_err);
            let msg = if join_err.is_panic() {
                "Worker thread panicked! Check logs."
            } else {
                "Worker thread cancelled."
            };
            response(
                StatusCode::INTERNAL_SERVER_ERROR,
                ApiResponse::failure(msg.to_string(), None)
            )
        }
    }
}
