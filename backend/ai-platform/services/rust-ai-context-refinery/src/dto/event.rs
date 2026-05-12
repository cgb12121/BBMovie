use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AssetEvent {
    #[serde(rename = "assetId")]
    pub asset_id: Uuid,
    #[serde(rename = "userId")]
    pub user_id: Uuid,
    pub bucket: String,
    #[serde(rename = "objectKey")]
    pub object_key: String,
    pub status: String,
    pub content: Option<String>,
}
