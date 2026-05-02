# Temporal orchestrator runbook

This module hosts the **Temporal workflow** for video transcoding and (optionally) a **NATS JetStream bridge** that starts workflows from MinIO `ObjectCreated` events.

Architecture background (repo paths):

- `media-content-platform/services/transcode-services/docs/01-TEMPORAL-ORCHESTRATION-ARCH.md`
- `media-content-platform/services/transcode-services/docs/02-WORKFLOW-AND-ACTIVITIES.md`

## Configuration

| Property | Purpose |
|----------|---------|
| `temporal.enabled` | When `false`, skips `WorkflowClient` / `WorkerFactory` and all workers (use in tests or when Temporal is unavailable). Default when unset: `true`. |
| `temporal.target` | gRPC target for Temporal frontend (e.g. `localhost:7233`). Env: `TEMPORAL_TARGET`. |
| `temporal.namespace` | Temporal namespace. Env: `TEMPORAL_NAMESPACE`. |
| `temporal.orchestrator-task-queue` | Task queue for `VideoProcessingWorkflow`. Env: `TEMPORAL_ORCHESTRATOR_TASK_QUEUE`. |
| `temporal.register-stub-activity-workers` | When `true`, registers the configured `MediaActivities` bean on `analysis-queue`, `encoding-queue`, `quality-queue`, `subtitle-queue`. Set `false` in production when separate workers poll those queues (e.g. **CAS** on `analysis-queue` — `java/cas/docs/README.md`; **VES** on `encoding-queue` — `java/ves/docs/README.md`). Env: `TEMPORAL_REGISTER_STUB_ACTIVITY_WORKERS`. |
| `temporal.activity-implementation` | `stub` (default): `StubMediaActivities`. `processing`: MinIO download/upload + FFmpeg/FFprobe (`ProcessingMediaActivities`). Env: `TEMPORAL_ACTIVITY_IMPLEMENTATION`. |
| `app.media-processing.*` | Used when `processing`: `hls-bucket`, `movies-key-prefix`, `temp-dir`, `ffmpeg-path`, `ffprobe-path`. |
| `app.transcode.nats-bridge.enabled` | When `true` **and** NATS `Connection` bean is created, starts the JetStream pull consumer and starts workflows. Env: `NATS_BRIDGE_ENABLED`. |
| `app.transcode.nats-bridge.max-ack-pending` | JetStream consumer `max_ack_pending` for the bridge consumer. Env: `NATS_BRIDGE_MAX_ACK_PENDING`. |
| `nats.url` | NATS server URL. Env: `NATS_URL`. |
| `nats.stream.name` / `nats.minio.subject` / `nats.consumer.durable` | Must match your JetStream stream and filter. Use a **different** `nats.consumer.durable` than `transcode-worker` if both run against the same stream (e.g. `temporal-orchestrator`). |

## Running beside the legacy `transcode-worker`

- Use a **separate durable consumer** name for this service (`nats.consumer.durable=temporal-orchestrator` by default) so messages are not split unpredictably between the legacy worker and the orchestrator.
- For cutover, stop the legacy consumer or disable its pipeline before pointing the same consumer at the new process.

## Workflow identity

- Workflow id: `transcode-{uploadId}`.
- Reuse policy: `WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE` so duplicate MinIO deliveries are ignored after the first successful start.

## Health

Actuator exposes `health` and `info` (`management.endpoints.web.exposure.include=health,info`).

## Tests

`src/test/resources/application.properties` sets `temporal.enabled=false` so Spring context tests do not require a real Temporal server. Workflow logic is covered with `io.temporal:temporal-testing` in `VideoProcessingWorkflowTest`.
