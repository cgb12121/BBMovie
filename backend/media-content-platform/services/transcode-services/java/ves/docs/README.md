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
- **VES:** This process **polls `encoding-queue` only** and executes the heavy encode step (FFmpeg integration is a follow-up; the current worker registers a placeholder implementation that returns deterministic paths for development).

Set **`temporal.register-stub-activity-workers=false`** on the orchestrator when dedicated workers are deployed (e.g. **CAS** on `analysis-queue` — `java/cas/docs/README.md`) so tasks are not double-consumed on **`encoding-queue`** or other queues.

## Configuration

| Property | Purpose |
|----------|---------|
| `temporal.enabled` | `false` skips Temporal client/worker (e.g. tests). Default: `true`. |
| `temporal.target` | Temporal frontend gRPC target (e.g. `localhost:7233`). |
| `temporal.namespace` | Temporal namespace. |

Actuator: enable `health` for readiness (see `application.properties`).

## Build

From `media-content-platform/services/transcode-services/java/`:

```text
mvn -pl transcode-contracts,ves -am verify
```

`transcode-contracts` must be built first (shared `MediaActivities` + DTOs).
