# VES — Video Encoding Service

## Names

| | |
|---|---|
| **Acronym** | **VES** |
| **Full name** | **Video Encoding Service** |
| **What it does** | Temporal **activity worker** for HLS transcoding: implements `encodeResolution` from `bbmovie.transcode.contracts.activity.MediaActivities` on task queue **`encoding-queue`**. |

Maven artifact / Spring application id: **`ves`** (`bbmovie.transcode:ves`, `spring.application.name=ves`).

## Where it fits

- **Orchestrator:** `temporal-orchestrator` runs `VideoProcessingWorkflow` and schedules `encodeResolution` activities on `encoding-queue` (see `docs/02-WORKFLOW-AND-ACTIVITIES.md` and `docs/04-ENCODER-WORKER-SPEC.md`).
- **VES:** This process **polls `encoding-queue` only** and executes `encodeResolution` with FFmpeg HLS packaging, uploads rendition playlists/segments to MinIO, and returns the emitted rendition playlist key for downstream manifest assembly.
- VES runs in stream-input mode: each encode attempt uses a presigned MinIO URL as FFmpeg input, while HLS output is produced locally then uploaded.
- VES emits Temporal activity heartbeats during long-running encode/upload phases and performs in-node retry for transient stream failures before returning failure.

Set **`temporal.register-stub-activity-workers=false`** on the orchestrator when dedicated workers are deployed (e.g. **CAS** on `analysis-queue` — `java/cas/docs/README.md`) so tasks are not double-consumed on **`encoding-queue`** or other queues.

## Configuration

| Property | Purpose |
|----------|---------|
| `temporal.enabled` | `false` skips Temporal client/worker (e.g. tests). Default: `true`. |
| `temporal.target` | Temporal frontend gRPC target (e.g. `localhost:7233`). |
| `temporal.namespace` | Temporal namespace. |
| `temporal.max-concurrent-activity-executions` | Per-node concurrency for encode activities on `encoding-queue` (default aggressive profile: `8`). |
| `temporal.max-concurrent-activity-task-pollers` | Number of parallel pollers fetching encode tasks from Temporal (default: `4`). |
| `ves.worker.register` | Register this service on `encoding-queue`. |
| `minio.url` / `minio.access-key` / `minio.secret-key` | MinIO connection for source download + HLS upload. |
| `app.media-processing.hls-bucket` | Bucket for encoded HLS outputs. |
| `app.media-processing.movies-key-prefix` | Object key prefix for HLS output tree. |
| `app.media-processing.ffmpeg-path` | FFmpeg binary path used by encoder worker. |
| `app.media-processing.ffmpeg-threads` | FFmpeg threads per encode task (default `2` for high node-level parallelism). |
| `app.media-processing.upload-parallelism` | Maximum concurrent MinIO uploads for HLS files per encode task. |
| `app.media-processing.stream-presign-expiry-seconds` | Presigned GET URL TTL for FFmpeg input stream. |
| `app.media-processing.stream-retry-attempts` | In-node retry attempts for transient stream input failures. |
| `app.media-processing.stream-retry-backoff-millis` | Backoff between in-node stream retries. |

Actuator: enable `health` for readiness (see `application.properties`).

## Build

From `media-content-platform/services/transcode-services/java/`:

```text
mvn -pl transcode-contracts,ves -am verify
```

`transcode-contracts` must be built first (shared `MediaActivities` + DTOs).
