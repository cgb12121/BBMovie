use axum::{
    http::StatusCode,
    response::{IntoResponse, Json},
};
use serde::Serialize;
use std::collections::HashMap;

#[derive(Serialize)]
pub struct ApiResponse<T> {
    pub success: bool,
    pub data: Option<T>,
    pub message: Option<String>,
    pub errors: Option<HashMap<String, String>>,
}

impl<T: Serialize> ApiResponse<T> {
    /// Success response with data
    pub fn success(data: T, msg: Option<String>) -> Self {
        Self {
            success: true,
            data: Some(data),
            message: msg,
            errors: None,
        }
    }

    /// Error response with message and optional details map
    pub fn failure(msg: String, errs: Option<HashMap<String, String>>) -> Self {
        Self {
            success: false,
            data: None,
            message: Some(msg),
            errors: errs,
        }
    }
}

/// Helper to return (StatusCode, JSON<ApiResponse>)
pub fn response<T: Serialize>(
    status: StatusCode,
    resp: ApiResponse<T>,
) -> impl IntoResponse {
    (status, Json(resp))
}
