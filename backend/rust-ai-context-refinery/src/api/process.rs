use std::path::PathBuf;
use axum::{
    http::StatusCode,
    response::IntoResponse,
    Json,
};
use serde_json::{json, Value};
use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};
use crate::dto::request::{ProcessBatchRequest, ProcessRequest};
use crate::dto::batch_result::BatchResultItem;

pub async fn handle_batch_process(
    Json(payload): Json<ProcessBatchRequest>
) -> impl IntoResponse {
    let mut results: Vec<BatchResultItem> = Vec::new();

    for req in payload.requests {
        let filename_backup = req.filename.clone();

        let result_item = match process_single_request(req).await {
            Ok(item) => item,
            Err(e) => {
                // If single request processing fails entirely, log and return an error for that item
                tracing::error!("Failed to process request for {}: {:?}", filename_backup, e);
                BatchResultItem {
                    filename: filename_backup,
                    result: None,
                    error: Some(format!("Processing failed: {}", e)),
                }
            }
        };
        results.push(result_item);
    }

    response(
        StatusCode::OK,
        ApiResponse::success(results, Some("Batch processing complete".to_string()))
    )
}

async fn process_single_request(req: ProcessRequest) -> anyhow::Result<BatchResultItem> {
    tracing::info!("Processing file from URL: {} (Type based on {})", req.file_url, req.filename);

    let (temp_path, _) = utils::download_file(&*req.file_url, &*req.filename).await?;
    let _guard = utils::TempFileGuard::new(temp_path.clone());
    
    // Determine a file type from extension
    let extension: String = temp_path.extension()
        .and_then(|s| s.to_str())
        .unwrap_or("")
        .to_lowercase();

    let service_result_mono = match extension.as_str() {
        "mp3" | "wav" | "m4a" => {
            tokio::task::spawn_blocking(move || services::whisper::run_whisper(&temp_path))
                .await?
                .map(|text| json!({"text": text}))
        },
        "png" | "jpg" | "jpeg" => {
            let path_for_thread: PathBuf = temp_path.clone();
            let ocr_result_mono: anyhow::Result<String> = tokio::task::spawn_blocking(move || services::ocr::run_ocr(&path_for_thread)).await?;
            let ocr_text: String = ocr_result_mono?;

            let vision_desc = services::vision::describe_image(&temp_path).await?;
            Ok(json!({"ocr_text": ocr_text.trim(), "vision_description": vision_desc}))
        },
        "pdf" => {
            tokio::task::spawn_blocking(move || services::pdf::extract_text_from_pdf(&temp_path))
                .await?
                .map(|text| json!({"text": text}))
        },
        "txt" | "md" | "json" | "xml" | "csv" => {
            tokio::task::spawn_blocking(move || services::text::extract_text_from_file(&temp_path))
                .await?
                .map(|text| json!({"text": text}))
        },
        _ => {
            return Ok(BatchResultItem {
                filename: req.filename,
                result: None,
                error: Some(format!("Unsupported file type: {}", extension)),
            });
        }
    };
    
    let result_json: Value = service_result_mono?;

    Ok(BatchResultItem {
        filename: req.filename,
        result: Some(result_json),
        error: None,
    })
}
