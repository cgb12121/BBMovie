# VVS — Video Validation Service

| Short name | **VVS** |
| Full name | **Video Validation Service** |

Spring Boot worker that registers `MediaActivities` on Temporal **`validation-queue`** and implements **`validateAndScore`** as a conformance/validation gate. Other activity methods fail fast if invoked.

Ported from legacy `transcode-worker` validation path. **VMAF** / full VQS scoring is not in this module.

## Configuration

- **`temporal.enabled`**: set `false` for local/tests without a Temporal server (no worker factory beans).
- **`vvs.worker.register`**: when `true` (default in `application.properties`) and Temporal is enabled, starts a worker on `validation-queue`.
- VVS and VQS are now designed to run together: VVS gates on `validation-queue`, VQS scores on `quality-queue`.

Requires MinIO credentials, `ffprobe` on `PATH` or `app.media-processing.ffprobe-path`, and network reachability to Temporal.
