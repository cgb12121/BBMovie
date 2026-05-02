# VVS — Video Validation Service

| Short name | **VVS** |
| Full name | **Video Validation Service** |

Spring Boot worker that registers `MediaActivities` on Temporal **`quality-queue`** and implements **`validateAndScore`** (ffprobe on the HLS playlist from MinIO; dimension tolerance vs `ValidationRequest`). Other activity methods fail fast if invoked.

Ported from legacy `transcode-worker` validation path. **VMAF** / full VQS scoring is not in this module.

## Configuration

- **`temporal.enabled`**: set `false` for local/tests without a Temporal server (no worker factory beans).
- **`vvs.worker.register`**: when `true` (default in `application.properties`) and Temporal is enabled, starts a worker on `quality-queue`.
- **`vqs.worker.register`** on the **VQS** app must stay **`false`** when VVS owns the queue in the same deployment (see `transcode-services/docs/05-QUALITY-WORKER-SPEC.md`).

Requires MinIO credentials, `ffprobe` on `PATH` or `app.media-processing.ffprobe-path`, and network reachability to Temporal.
