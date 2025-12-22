use axum::{http::StatusCode, response::IntoResponse, Extension, Json};
use serde_json::{json, Value};
use tracing::log::__private_api::log;
use crate::{services, utils};
use crate::dto::response::{ApiResponse, response};
use crate::dto::request::{ProcessBatchRequest, ProcessRequest};
use crate::dto::batch_result::BatchResultItem;
use crate::services::redis::CacheService;

pub async fn handle_batch_process(
    Extension(cache_service): Extension<CacheService>,
    Json(payload): Json<ProcessBatchRequest>
) -> impl IntoResponse {
    let mut results: Vec<BatchResultItem> = Vec::new();
    let mut success_count: i8 = 0;
    let mut fail_count: i8 = 0;

    for req in payload.requests {
        let filename_backup = req.filename.clone();

        let result_item = match process_single_request(req, cache_service.clone()).await {
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

async fn process_single_request(req: ProcessRequest, cache_service: CacheService) -> anyhow::Result<BatchResultItem> {
    tracing::info!("Processing file from URL: {} (Type based on {})", req.file_url, req.filename);

    // First, check if we have the result in the cache
    if let Some(cached_result) = cache_service.get_compressed(&req.filename).await? {
        tracing::info!("Cache HIT for filename: {}", req.filename);
        return Ok(BatchResultItem {
            filename: req.filename,
            result: Some(cached_result.parse()?),
            error: None,
        });
    }

    let (temp_path, _) = utils::download_file(&req.file_url, &req.filename).await.map_err(|e| {
        tracing::error!("Download failed for {}: {}", req.filename, e);
        anyhow::anyhow!("Download failed: {}", e)
    })?;
    // Vệ sĩ dọn rác (RAII)
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
            // Chạy OCR song song Vision (nếu muốn tối ưu thêm thì dùng tokio::join!)
            // Ở đây chạy tuần tự cho an toàn RAM
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
        // set_compressed nhận (filename, &str)
        if let Err(e) = cache.set_compressed(&fname, &data_to_cache).await {
            tracing::warn!("Cache failed for {}: {}", fname, e);
        }
    });

    Ok(BatchResultItem {
        filename: req.filename,
        result: Some(result_value),
        error: None,
    })
}
