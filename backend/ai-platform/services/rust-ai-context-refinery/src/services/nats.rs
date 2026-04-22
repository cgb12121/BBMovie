use async_nats::Client;
use serde_json::json;
use tracing::{error, info};
use std::sync::Arc;

#[derive(Clone)]
pub struct NatsService {
    client: Option<Arc<Client>>,
}

impl NatsService {
    pub async fn new(nats_url: &str) -> Self {
        match async_nats::connect(&nats_url).await {
            Ok(client) => {
                info!("Connected to NATS at {}", nats_url);
                Self { client: Some(Arc::new(client)) }
            }
            Err(e) => {
                error!("Failed to connect to NATS at {}: {}. Status updates will be disabled.", nats_url, e);
                Self { client: None }
            }
        }
    }

    pub async fn publish_media_status_update(
        &self,
        upload_id: &str,
        status: &str,
    ) {
        if let Some(ref client) = self.client {
            let event = json!({
                "uploadId": upload_id,
                "status": status
            });

            match client.publish("media.status.update", event.to_string().into()).await {
                Ok(_) => {
                    info!("Published status update: uploadId={}, status={}", upload_id, status);
                }
                Err(e) => {
                    error!("Failed to publish status update for uploadId {}: {}", upload_id, e);
                }
            }
        } else {
            tracing::debug!("NATS client not available, skipping status update for uploadId={}", upload_id);
        }
    }
}

