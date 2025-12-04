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
    // 1. Save uploaded file to secure temp file (Issue 15)
    // We now get a NamedTempFile which will self-delete on Drop
    let (temp_file, filename) = match utils::save_temp_file(&mut multipart).await {
        Ok(res) => res,
        Err(e) => {
            return response(
                StatusCode::BAD_REQUEST,
                ApiResponse::failure(e.to_string(), None)
            );
        }
    };

    tracing::info!("Processing PDF: {}", filename);
    
    // 2. Process via service layer (Blocking Task)
    // We keep 'temp_file' alive in this async scope.
    // We pass the path to the blocking task.
    let processing_path = temp_file.path().to_owned();
    let result = tokio::task::spawn_blocking(move || {
        services::pdf::extract_text_from_pdf(&processing_path)
    }).await;

    // 3. Cleanup is handled AUTOMATICALLY when 'temp_file' goes out of scope here!
    // However, Issue 11 requested explicit cleanup/guard. 
    // Since NamedTempFile is RAII, it *is* the guard.
    // But to be extra verbose about "Issue 7: Log warning on failure",
    // NamedTempFile ignores errors on Drop.
    // If we want to log errors, we must close explicitly.
    if let Err(e) = temp_file.close() {
         tracing::warn!("Failed to delete temp file for PDF: {}", e);
    }

    // 4. Handle Result
    match result {
        Ok(service_res) => match service_res {
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
            )
        },
        Err(_) => response(
            StatusCode::INTERNAL_SERVER_ERROR,
            ApiResponse::failure("PDF worker panicked or was cancelled".to_string(), None)
        )
    }
}
