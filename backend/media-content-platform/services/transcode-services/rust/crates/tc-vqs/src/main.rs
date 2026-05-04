//! VQS — ffprobe playlist + stub VMAF score; stdout **QualityReport** (camelCase like Java).

use std::env;
use std::error::Error;
use std::fs;

use tc_runtime::config::RuntimeConfig;
use tc_runtime::storage::StorageClient;
use tc_vqs::{score_from_storage_and_vmaf, score_playlist};
use transcode_contracts::dto::ValidationRequest;
use transcode_contracts::temporal;

#[cfg(feature = "temporal-runtime")]
use std::sync::Arc;
#[cfg(feature = "temporal-runtime")]
use temporalio_client::{Client, ClientOptions, Connection};
#[cfg(feature = "temporal-runtime")]
use temporalio_common::envconfig::LoadClientConfigProfileOptions;
#[cfg(feature = "temporal-runtime")]
use temporalio_macros::activities;
#[cfg(feature = "temporal-runtime")]
use temporalio_sdk::activities::{ActivityContext, ActivityError};
#[cfg(feature = "temporal-runtime")]
use temporalio_sdk::{Worker, WorkerOptions};
#[cfg(feature = "temporal-runtime")]
use temporalio_sdk_core::{CoreRuntime, RuntimeOptions};

#[cfg(feature = "temporal-runtime")]
#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    run_main().await
}

#[cfg(not(feature = "temporal-runtime"))]
fn main() -> Result<(), Box<dyn Error>> {
    futures::executor::block_on(run_main())
}

async fn run_main() -> Result<(), Box<dyn Error>> {
    let cfg = RuntimeConfig::from_env();
    if env::var("RUN_MODE").ok().as_deref() == Some("worker") {
        #[cfg(feature = "temporal-runtime")]
        return run_worker_mode(cfg).await;
        #[cfg(not(feature = "temporal-runtime"))]
        {
            eprintln!("worker mode requires --features temporal-runtime (and protoc installed)");
            return Ok(());
        }
    }
    run_cli_mode(&cfg)
}

fn run_cli_mode(cfg: &RuntimeConfig) -> Result<(), Box<dyn Error>> {
    let args: Vec<String> = env::args().collect();
    if args.len() != 3 {
        eprintln!(
            "usage: {} <playlist.m3u8> <validation-request.json>\n\
             env: FFPROBE_PATH (default: ffprobe)\n\
             env: USE_LIBVMAF (1 enables ffmpeg/libvmaf scoring)\n\
             stdout: QualityReport — task queue {}",
            args.first().map(String::as_str).unwrap_or("vqs"),
            temporal::QUALITY
        );
        std::process::exit(2);
    }
    let playlist = &args[1];
    let json = fs::read_to_string(&args[2])?;
    let req: ValidationRequest = serde_json::from_str(&json)?;
    let report = if env::var("USE_LIBVMAF").ok().as_deref() == Some("1") {
        let storage = StorageClient::new(cfg.clone())?;
        let mut req2 = req.clone();
        req2.playlist_path = playlist.clone();
        score_from_storage_and_vmaf(cfg, &storage, &req2)
    } else {
        score_playlist(&cfg.ffprobe_path, playlist, &req)
    };
    println!("{}", serde_json::to_string_pretty(&report)?);
    Ok(())
}

#[cfg(feature = "temporal-runtime")]
async fn run_worker_mode(cfg: RuntimeConfig) -> Result<(), Box<dyn Error>> {
    if !cfg.temporal_enabled {
        eprintln!("temporal disabled; not starting vqs worker");
        return Ok(());
    }
    if !cfg.vqs_worker_register {
        eprintln!("vqs worker registration disabled by VQS_WORKER_REGISTER");
        return Ok(());
    }

    let runtime = CoreRuntime::new_assume_tokio(RuntimeOptions::builder().build()?)?;
    let (connection_options, client_options) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, client_options)?;

    let storage = StorageClient::new(cfg.clone())?;
    let activities = VqsActivities {
        cfg: Arc::new(cfg),
        storage: Arc::new(storage),
    };
    let worker_options = WorkerOptions::new(temporal::QUALITY)
        .register_activities(activities)
        .build();
    println!("vqs-worker running on queue={}", temporal::QUALITY);
    Worker::new(&runtime, client, worker_options)?.run().await?;
    Ok(())
}

#[cfg(feature = "temporal-runtime")]
pub struct VqsActivities {
    cfg: Arc<RuntimeConfig>,
    storage: Arc<StorageClient>,
}

#[cfg(feature = "temporal-runtime")]
#[activities]
impl VqsActivities {
    #[activity(name = "validateAndScore")]
    pub async fn validate_and_score(
        &self,
        _ctx: ActivityContext,
        req: ValidationRequest,
    ) -> Result<transcode_contracts::dto::QualityReport, ActivityError> {
        Ok(score_from_storage_and_vmaf(&self.cfg, &self.storage, &req))
    }
}
