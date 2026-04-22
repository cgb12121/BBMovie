use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BatchResultItem {
    pub filename: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>, // Can hold text, description, etc.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}
