use std::env;

#[derive(Debug, Clone)]
pub struct RuntimeConfig {
    pub ffprobe_path: String,
    pub ffmpeg_path: String,
    pub minio_endpoint: String,
    pub minio_access_key: String,
    pub minio_secret_key: String,
    pub minio_region: String,
    pub hls_bucket: String,
    pub source_bucket: String,
    pub vvs_worker_register: bool,
    pub vqs_worker_register: bool,
    pub temporal_enabled: bool,
    pub temporal_namespace: String,
    pub temporal_target: String,
    pub quality_reference_template: String,
}

impl RuntimeConfig {
    pub fn from_env() -> Self {
        Self {
            ffprobe_path: env_or("FFPROBE_PATH", "ffprobe"),
            ffmpeg_path: env_or("FFMPEG_PATH", "ffmpeg"),
            minio_endpoint: env_or("MINIO_API_URL", "http://localhost:9000"),
            minio_access_key: env_or("MINIO_ACCESS_KEY", "minioadmin"),
            minio_secret_key: env_or("MINIO_SECRET_KEY", "minioadmin"),
            minio_region: env_or("MINIO_REGION", "us-east-1"),
            hls_bucket: env_or("HLS_BUCKET", "bbmovie-hls"),
            source_bucket: env_or("SOURCE_BUCKET", "bbmovie-source"),
            vvs_worker_register: env_flag("VVS_WORKER_REGISTER", true),
            vqs_worker_register: env_flag("VQS_WORKER_REGISTER", false),
            temporal_enabled: env_flag("TEMPORAL_ENABLED", true),
            temporal_namespace: env_or("TEMPORAL_NAMESPACE", "default"),
            temporal_target: env_or("TEMPORAL_TARGET", "localhost:7233"),
            quality_reference_template: env_or(
                "QUALITY_REFERENCE_TEMPLATE",
                "uploads/{upload_id}/source.mp4",
            ),
        }
    }

    pub fn reference_key_for_upload(&self, upload_id: &str) -> String {
        self.quality_reference_template
            .replace("{upload_id}", upload_id)
    }
}

fn env_or(key: &str, default_value: &str) -> String {
    env::var(key).unwrap_or_else(|_| default_value.to_string())
}

fn env_flag(key: &str, default_value: bool) -> bool {
    env::var(key)
        .ok()
        .and_then(|v| match v.trim().to_ascii_lowercase().as_str() {
            "1" | "true" | "yes" | "on" => Some(true),
            "0" | "false" | "no" | "off" => Some(false),
            _ => None,
        })
        .unwrap_or(default_value)
}

