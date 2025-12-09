use axum::{Extension, Router, extract::DefaultBodyLimit, routing::{get, post}};
use tower_http::trace::TraceLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt, EnvFilter};

use ai_refinery::{infra, api, services};

#[tokio::main]
async fn main() {
    // Load env vars
    dotenvy::dotenv().ok();

    // Configure logging
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| "ai_refinery=debug,lopdf=error,tower_http=debug,info".into());
    tracing_subscriber::registry()
            .with(tracing_subscriber::fmt::layer())
            .with(filter)
            .init();

    //TODO: remove 🔥 EAGER INIT in prod
    tracing::info!("Warming up Whisper Engine...");
    if let Err(e) = services::whisper::eager_init() {
        tracing::error!("Failed to load Whisper Model: {}", e);
        std::process::exit(1);
    }
    tracing::info!("Whisper Engine is ready and hot!");

    // Eureka Client (fire-and-forget)
    infra::eureka::register_and_heartbeat();

    let redis_url = std::env::var("REDIS_URL")
        .unwrap_or_else(|_| "redis://127.0.0.1:6379".to_string());
    let cache_service = services::redis::CacheService::new(&redis_url);

    // 2. Setup Routes
    let app = Router::new()
        .route("/health", get(|| async { "Rust Worker is UP!" }))
        .route("/info", get(||async { "Rust Worker is UP!" }))
        // New Unified Batch Processing Route
        .route("/api/process-batch", post(api::handle_batch_process))
        .layer(Extension(cache_service))
        // Limit upload size 50MB (prevent OOM)
        .layer(DefaultBodyLimit::max(50 * 1024 * 1024))
        .layer(TraceLayer::new_for_http());

    // 3. Start Server
    let port = std::env::var("PORT").unwrap_or_else(|_| "8686".to_string());
    let addr_str = format!("0.0.0.0:{}", port);
    tracing::info!("Rust Worker listening on {}", addr_str);

    let listener = match tokio::net::TcpListener::bind(&addr_str).await {
        Ok(l) => l,
        Err(e) => {
            tracing::error!("Failed to bind to address {}: {}", addr_str, e);
            std::process::exit(1);
        }
    };

    if let Err(e) = axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await {
            tracing::error!("Server failed to start: {}", e);
            std::process::exit(1);
        }
}

async fn shutdown_signal() {
    let ctrl_c = async {
        tokio::signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("failed to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
    tracing::info!("Signal received, starting graceful shutdown...");
}
