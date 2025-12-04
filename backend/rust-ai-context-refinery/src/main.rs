use axum::{
    Router, extract::DefaultBodyLimit, routing::{get, post}
};
use std::net::SocketAddr;
use tower_http::trace::TraceLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt, EnvFilter};

use ai_refinery::{infra, api};

#[tokio::main]
async fn main() {
    // Configure logging
    // Setting rules:
    // 1. "rust_ai_context_refinery=debug": Detailed logs for the main application (debug)
    // 2. "lopdf=error": lopdf only shows error logs (NO info/warn)
    // 3. "info": Other libs only info
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| "rust_ai_context_refinery=debug,lopdf=error,tower_http=debug,info".into());
    tracing_subscriber::registry()
            .with(tracing_subscriber::fmt::layer())
            .with(filter)
            .init();

    // Eureka Client (async, non block server)
    infra::eureka::register_and_heartbeat().await;

    // 2. Setup Routes (Like @RestController) - keep the HTTP layer thin,
    // all heavy work is delegated to the handlers and services modules.
    let app = Router::new()
        .route("/health", get(|| async { "Rust Worker is UP!" }))       // ✅
        .route("/api/extract/pdf", post(api::handle_pdf_extract))       // ✅
        .route("/api/extract/ocr", post(api::handle_ocr))               // ✅
        .route("/api/extract/text", post(api::handle_text_extract))     // ✅
        .route("/api/transcribe", post(api::handle_transcribe))         // ✅
        .route("/api/analyze/image", post(api::handle_image_analysis))  // ✅
        // Limit upload size 50MB (prevent OOM)
        .layer(DefaultBodyLimit::max(50 * 1024 * 1024))
        .layer(TraceLayer::new_for_http());

    // 3. Start Server port 8686
    let addr = SocketAddr::from(([0, 0, 0, 0], 8686));
    tracing::info!("Rust Worker listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}