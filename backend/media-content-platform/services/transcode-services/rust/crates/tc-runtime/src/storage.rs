use std::time::Duration;

use anyhow::{Context, Result};
use reqwest::blocking::Client;
use s3::bucket::Bucket;
use s3::creds::Credentials;
use s3::region::Region;

use crate::config::RuntimeConfig;

#[derive(Clone)]
pub struct StorageClient {
    cfg: RuntimeConfig,
    http: Client,
}

impl StorageClient {
    pub fn new(cfg: RuntimeConfig) -> Result<Self> {
        let http = Client::builder()
            .timeout(Duration::from_secs(120))
            .build()
            .context("build reqwest client")?;
        Ok(Self { cfg, http })
    }

    pub fn download_object(&self, bucket: &str, key: &str) -> Result<Vec<u8>> {
        let runtime = tokio::runtime::Runtime::new().context("tokio runtime")?;
        runtime.block_on(self.download_object_async(bucket, key))
    }

    pub fn download_partial(&self, bucket: &str, key: &str, max_bytes: usize) -> Result<Vec<u8>> {
        let runtime = tokio::runtime::Runtime::new().context("tokio runtime")?;
        runtime.block_on(self.download_partial_async(bucket, key, max_bytes))
    }

    pub fn presign_get(&self, bucket: &str, key: &str, expiry_secs: u32) -> Result<String> {
        let b = self.bucket(bucket)?;
        let runtime = tokio::runtime::Runtime::new().context("tokio runtime")?;
        runtime
            .block_on(b.presign_get(key, expiry_secs, None))
            .context("presign get")
    }

    fn bucket(&self, name: &str) -> Result<Box<Bucket>> {
        let region = Region::Custom {
            region: self.cfg.minio_region.clone(),
            endpoint: self.cfg.minio_endpoint.clone(),
        };
        let creds = Credentials::new(
            Some(&self.cfg.minio_access_key),
            Some(&self.cfg.minio_secret_key),
            None,
            None,
            None,
        )
        .context("build s3 credentials")?;
        let bucket = Bucket::new(name, region, creds)
            .context("create bucket")?
            .with_path_style();
        Ok(bucket)
    }

    async fn download_object_async(&self, bucket: &str, key: &str) -> Result<Vec<u8>> {
        let b = self.bucket(bucket)?;
        let response = b
            .get_object(key)
            .await
            .with_context(|| format!("s3 get_object {bucket}/{key}"))?;
        if response.status_code() != 200 {
            anyhow::bail!(
                "s3 get_object failed {} for {}/{}",
                response.status_code(),
                bucket,
                key
            );
        }
        Ok(response.bytes().to_vec())
    }

    async fn download_partial_async(&self, bucket: &str, key: &str, max_bytes: usize) -> Result<Vec<u8>> {
        let b = self.bucket(bucket)?;
        let end = if max_bytes == 0 { 0 } else { max_bytes - 1 } as u64;
        let response = b
            .get_object_range(key, 0, Some(end))
            .await
            .with_context(|| format!("s3 get_object_range {bucket}/{key}"))?;
        if !(response.status_code() == 200 || response.status_code() == 206) {
            anyhow::bail!(
                "s3 get_object_range failed {} for {}/{}",
                response.status_code(),
                bucket,
                key
            );
        }
        Ok(response.bytes().to_vec())
    }

    pub fn download_via_url(&self, url: &str) -> Result<Vec<u8>> {
        let resp = self
            .http
            .get(url)
            .send()
            .with_context(|| format!("http get {url}"))?;
        if !resp.status().is_success() {
            anyhow::bail!("http get failed {} for {}", resp.status(), url);
        }
        resp.bytes()
            .map(|b| b.to_vec())
            .context("read http response bytes")
    }
}

