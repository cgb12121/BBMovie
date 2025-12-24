use serde::Deserialize;

#[derive(Deserialize)]
pub struct ProcessRequest {
    pub file_url: String, 
    pub filename: String,
    #[serde(default)]
    pub upload_id: Option<String>, // Optional uploadId for status updates
}

#[derive(Deserialize)]
pub struct ProcessBatchRequest {
    pub requests: Vec<ProcessRequest>
}