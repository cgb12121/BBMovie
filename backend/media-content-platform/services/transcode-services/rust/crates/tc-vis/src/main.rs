//! VIS — probe media and print **VideoMetadata** JSON (Java `MetadataDTO`: width, height, durationSeconds, codec).

use std::error::Error;
use std::env;

use tc_runtime::config::RuntimeConfig;
use tc_lgs::ladder_for_metadata;
use tc_vis::{default_fast_probe, ProbeRequest};
use transcode_contracts::temporal;

#[cfg(feature = "temporal-runtime")]
use std::sync::Arc;
#[cfg(feature = "temporal-runtime")]
use tc_vis::FastProbeService;
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
use transcode_contracts::dto::VideoMetadata;

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
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        eprintln!(
            "usage: {} <path-or-url>\n\
             optional: {} <path-or-url> <object-key>\n\
             env: FFPROBE_PATH (default: ffprobe)\n\
             env: PROBE_PARTIAL_MB (default: 10)\n\
             stdout: JSON VideoMetadata — same field names as Java MetadataDTO\n\
             stderr: ladder summary (would use Temporal task queue {})",
            args.get(0).map(String::as_str).unwrap_or("vis"),
            args.get(0).map(String::as_str).unwrap_or("vis"),
            temporal::ANALYSIS
        );
        std::process::exit(2);
    }
    let input = args[1].clone();
    let key = args.get(2).cloned().unwrap_or_else(|| infer_key(&input));
    let bucket = env::var("PROBE_BUCKET").unwrap_or_else(|_| cfg.source_bucket.clone());
    let partial_mb = env::var("PROBE_PARTIAL_MB")
        .ok()
        .and_then(|v| v.parse::<usize>().ok())
        .unwrap_or(10);
    let probe = default_fast_probe(cfg.ffprobe_path.clone(), partial_mb);
    let outcome = probe
        .probe(&ProbeRequest {
            bucket,
            key,
            source: Some(input),
        })
        .expect("probe failed");
    println!(
        "{}",
        serde_json::to_string_pretty(&outcome.metadata).expect("serialize VideoMetadata")
    );
    let ladder = ladder_for_metadata(&outcome.metadata);
    eprintln!(
        "# ladder (task queue {}): strategy={} rungs={}",
        temporal::ANALYSIS,
        outcome.strategy,
        ladder.len()
    );
    for r in ladder {
        eprintln!("#   {} {}x{} cost={}", r.label, r.width, r.height, r.relative_cost);
    }
    Ok(())
}

fn infer_key(input: &str) -> String {
    if let Some((_, tail)) = input.rsplit_once('/') {
        tail.to_lowercase()
    } else {
        input.to_lowercase()
    }
}

#[cfg(feature = "temporal-runtime")]
async fn run_worker_mode(cfg: RuntimeConfig) -> Result<(), Box<dyn Error>> {
    if !cfg.temporal_enabled {
        eprintln!("temporal disabled; not starting vis worker");
        return Ok(());
    }

    let runtime = CoreRuntime::new_assume_tokio(RuntimeOptions::builder().build()?)?;
    let (connection_options, client_options) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, client_options)?;

    let probe = default_fast_probe(cfg.ffprobe_path.clone(), 10);
    let activities = VisActivities {
        probe: Arc::new(probe),
        source_bucket: cfg.source_bucket.clone(),
    };
    let worker_options = WorkerOptions::new(temporal::ANALYSIS)
        .register_activities(activities)
        .build();
    println!("vis-worker running on queue={}", temporal::ANALYSIS);
    Worker::new(&runtime, client, worker_options)?.run().await?;
    Ok(())
}

#[cfg(feature = "temporal-runtime")]
#[derive(Clone, Debug, serde::Serialize, serde::Deserialize)]
pub struct AnalyzeSourceRequest {
    pub upload_id: String,
    pub bucket: String,
    pub key: String,
}

#[cfg(feature = "temporal-runtime")]
pub struct VisActivities {
    probe: Arc<FastProbeService>,
    source_bucket: String,
}

#[cfg(feature = "temporal-runtime")]
#[activities]
impl VisActivities {
    #[activity(name = "analyzeSource")]
    pub async fn analyze_source(
        &self,
        _ctx: ActivityContext,
        req: AnalyzeSourceRequest,
    ) -> Result<VideoMetadata, ActivityError> {
        let bucket = if req.bucket.is_empty() {
            self.source_bucket.clone()
        } else {
            req.bucket.clone()
        };
        let out = self
            .probe
            .probe(&ProbeRequest {
                bucket,
                key: req.key,
                source: None,
            })
            .map_err(|e| ActivityError::ApplicationFailure(e.to_string(), false))?;
        Ok(out.metadata)
    }
}
