use deadpool_redis::{Config, Runtime, Pool};
use redis::AsyncCommands;
use serde_json::Value;
use anyhow::Result;
use async_compression::tokio::write::GzipEncoder;
use async_compression::futures::bufread::GzipDecoder;
use tokio::io::AsyncWriteExt;
use futures::io::{AsyncReadExt, Cursor};

#[derive(Clone)]
pub struct CacheService {
    pool: Pool,
}

impl CacheService {
    // 1. Khởi tạo Pool kết nối
    pub fn new(redis_url: &str) -> Self {
        let cfg = Config::from_url(redis_url);
        let pool = cfg.create_pool(Some(Runtime::Tokio1))
            .expect("❌ Failed to create Redis pool");

        Self { pool }
    }
    // 2. Tạo Key chuẩn: "refinery:result:{filename}"
    fn generate_key(&self, filename: &str) -> String {
        format!("refinery:result:{}", filename)
    }

    pub async fn set_compressed(&self, filename: &str, text: &str) -> Result<()> {
        let mut encoder = GzipEncoder::new(Vec::new());
        encoder.write_all(text.as_bytes()).await?;
        encoder.shutdown().await?;
        let compressed_bytes = encoder.into_inner();

        let mut conn = self.pool.get().await?;
        let key = self.generate_key(filename);

        // Lưu dưới dạng Bytes (Redis hỗ trợ binary safe)
        conn.set_ex::<_, _, ()>(key, compressed_bytes, 86400).await?;
        tracing::debug!("💾 Cached compressed result for: {}", filename);
        Ok(())
    }

    pub async fn get_compressed(&self, filename: &str) -> Result<Option<String>> {
        let mut conn = self.pool.get().await?;
        let key = self.generate_key(filename);

        // Lấy dữ liệu nén từ Redis
        let compressed_bytes: Option<Vec<u8>> = conn.get(&key).await?;

        if let Some(bytes) = compressed_bytes {
            tracing::info!("🎯 Compressed Cache HIT for: {}", filename);

            // Giải nén dữ liệu
            let cursor = Cursor::new(bytes);
            let mut decoder = GzipDecoder::new(cursor);
            let mut buffer = Vec::new();
            decoder.read_to_end(&mut buffer).await?;

            let text = String::from_utf8(buffer)?;
            return Ok(Some(text));
        }

        tracing::info!("💨 Compressed Cache MISS for: {}", filename);
        Ok(None)
    }
}