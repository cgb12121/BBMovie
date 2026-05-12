use async_nats::Client;
use futures::StreamExt;
use tracing::{error, info};
use std::sync::Arc;
use crate::services::processor;
use crate::dto::event::AssetEvent;

pub async fn start_worker(client: Arc<Client>) {
    let subject = "asset.process.trigger";
    let mut subscriber = match client.subscribe(subject).await {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to subscribe to {}: {}", subject, e);
            return;
        }
    };

    info!("Rust Ingestion Worker listening on {}", subject);

    while let Some(message) = subscriber.next().await {
        let client_clone = client.clone();
        tokio::spawn(async move {
            if let Ok(event) = serde_json::from_slice::<AssetEvent>(&message.payload) {
                info!("Processing asset event: {}", event.asset_id);
                
                match processor::process_asset(&event).await {
                    Ok(content) => {
                        let mut completed_event = event;
                        completed_event.content = Some(content);
                        completed_event.status = "INGESTED".to_string();

                        let payload = serde_json::to_vec(&completed_event).unwrap();
                        if let Err(e) = client_clone.publish("asset.process.completed", payload.into()).await {
                            error!("Failed to publish completion for {}: {}", completed_event.asset_id, e);
                        } else {
                            info!("Successfully processed and notified for asset: {}", completed_event.asset_id);
                        }
                    }
                    Err(e) => {
                        error!("Failed to process asset event {}: {}", event.asset_id, e);
                    }
                }
            } else {
                error!("Failed to parse asset event from NATS");
            }
        });
    }
}
