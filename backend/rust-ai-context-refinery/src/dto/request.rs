use serde::Deserialize;

#[derive(Deserialize)]
pub struct ProcessRequest {
    pub file_url: String, 
    pub filename: String,
}

#[derive(Deserialize)]
pub struct ProcessBatchRequest {
    pub requests: Vec<ProcessRequest>
}