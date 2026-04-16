use axum::{http::StatusCode, response::IntoResponse, Extension, Json};
use serde_json::json;
use crate::{services, utils};
use crate::dto::response::ApiResponse;
use crate::dto::request::{ProcessBatchRequest, ProcessRequest};
use crate::dto::batch_result::BatchResultItem;
use crate::services::redis::CacheService;
use crate::services::nats::NatsService;

pub async fn handle_batch_process(
    Extension(cache_service): Extension<CacheService>,
    Extension(nats_service): Extension<NatsService>,
    Json(payload): Json<ProcessBatchRequest>
) -> impl IntoResponse {
    let mut results: Vec<BatchResultItem> = Vec::new();
    let mut success_count: i8 = 0;
    let mut fail_count: i8 = 0;

    for req in payload.requests {
        let filename_backup = req.filename.clone();

        let result_item = match process_single_request(req, cache_service.clone(), &nats_service).await {
            Ok(item) => {
                success_count += 1;
                item
            },
            Err(e) => {
                fail_count += 1;
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

    let status_code: StatusCode = if fail_count == 0 {
        // All success -> 200 OK
        StatusCode::OK
    } else if success_count == 0 {
        // All failed -> 422
        StatusCode::UNPROCESSABLE_ENTITY
    } else {
        // Some success, some failed-> 207 Multi-Status
        StatusCode::MULTI_STATUS
    };

    (
        status_code,
        Json(ApiResponse::success(results, Some(format!(
            "Batch complete. Success: {}, Failed: {}", success_count, fail_count
        ))))
    )
}

async fn process_single_request(
    req: ProcessRequest, 
    cache_service: CacheService, 
    nats_service: &NatsService
) -> anyhow::Result<BatchResultItem> {
    tracing::info!("Processing file: {} (upload_id: {:?})", req.filename, req.upload_id);

    // First, check if we have the result in the cache
    if let Some(cached_result) = cache_service.get_compressed(&req.filename).await? {
        tracing::info!("Cache HIT for filename: {}", req.filename);
        if let Some(ref upload_id) = req.upload_id {
            nats_service.publish_media_status_update(upload_id, "VALIDATED").await;
        }
        return Ok(BatchResultItem {
            filename: req.filename,
            result: Some(cached_result.parse()?),
            error: None,
        });
    }

    let temp_path = if let Some(base64_str) = req.base64_content {
        tracing::debug!("Using provided base64 content for {}", req.filename);
        let bytes = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, base64_str)
            .map_err(|e| anyhow::anyhow!("Base64 decode failed: {}", e))?;
        
        let extension = std::path::Path::new(&req.filename)
            .extension()
            .and_then(|ext| ext.to_str())
            .map(|ext| format!(".{}", ext))
            .unwrap_or_default();

        let temp_name = format!("b64_{}_{}", uuid::Uuid::new_v4(), extension);
        let path = std::env::temp_dir().join(&temp_name);
        tokio::fs::write(&path, bytes).await?;
        path
    } else if let Some(url) = req.file_url {
        tracing::debug!("Downloading file from URL: {}", url);
        let (path, _) = utils::download_file(&url, &req.filename).await.map_err(|e| {
            tracing::error!("Download failed for {}: {}", req.filename, e);
            anyhow::anyhow!("Download failed: {}", e)
        })?;
        path
    } else {
        return Err(anyhow::anyhow!("Neither file_url nor base64_content provided"));
    };

    // RAII GUARD auto delete temp file as it is dropped outside the scope
    let _guard = utils::TempFileGuard::new(temp_path.clone());

    // Determine a file type from extension
    let extension: String = temp_path.extension()
        .and_then(|s| s.to_str())
        .unwrap_or("")
        .to_lowercase();

    let result_value = match extension.as_str() {
        "mp3" | "wav" | "m4a" => {
            let path = temp_path.clone();
            let text = tokio::task::spawn_blocking(move || services::whisper::run_whisper(&path)).await??;
            json!({ "text": text })
        },
        "png" | "jpg" | "jpeg" => {
            let path_ocr = temp_path.clone();
            // Run OCR in parallel with Vision, can use tokio::join!! for more performance
            // But we will run on sequence to save RAM
            let ocr_text = tokio::task::spawn_blocking(move || services::ocr::run_ocr(&path_ocr)).await??;
            let vision_desc = services::vision::describe_image(&temp_path).await?;

            json!({
                "ocr_text": ocr_text.trim(),
                "vision_description": vision_desc
            })
        },
        "pdf" => {
            let path = temp_path.clone();
            let text = tokio::task::spawn_blocking(move || services::pdf::extract_text_from_pdf(&path)).await??;
            json!({ "text": text })
        },
        "txt" | "md" | "json" | "xml" | "csv" => {
            let path = temp_path.clone();
            let text = tokio::task::spawn_blocking(move || services::text::extract_text_from_file(&path)).await??;
            json!({ "text": text })
        },
        _ => {
            return Ok(BatchResultItem {
                filename: req.filename,
                result: None,
                error: Some(format!("Unsupported file type: .{}", extension)),
            });
        }
    };

    let json_string = result_value.to_string();

    let cache = cache_service.clone();
    let fname = req.filename.clone();
    let data_to_cache = json_string.clone();

    tokio::spawn(async move {
        // set_compressed receive (filename, &str)
        if let Err(e) = cache.set_compressed(&fname, &data_to_cache).await {
            tracing::warn!("Cache failed for {}: {}", fname, e);
        }
    });

    // Publish status update after successful processing (not cache hit)
    if let Some(ref upload_id) = req.upload_id {
        nats_service.publish_media_status_update(upload_id, "VALIDATED").await;
    }

    Ok(BatchResultItem {
        filename: req.filename,
        result: Some(result_value),
        error: None,
    })
}
