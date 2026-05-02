# CAS — Complex Analysis Service

## Names

| | |
|---|---|
| **Acronym** | **CAS** |
| **Full name** | **Complex Analysis Service** |
| **What it does** | Temporal **activity worker** on **`analysis-queue`**: implements `analyzeSource`, `generateMasterManifest`, and `integrateSubtitles` from `bbmovie.transcode.contracts.activity.MediaActivities` (per `docs/02-WORKFLOW-AND-ACTIVITIES.md`). |

Maven artifact / Spring application id: **`cas`** (`bbmovie.transcode:cas`, `spring.application.name=cas`).

The analyzer spec (`docs/03-ANALYZER-WORKER-SPEC.md`) also describes **VIS** (video inspection via `ffprobe`) and complexity heuristics as logical parts of analysis; this service is the deployable **CAS** process that hosts those **analysis-queue** activities until split further (e.g. dedicated VIS).

## Where it fits

- **Orchestrator:** `VideoProcessingWorkflowImpl` calls `analysis.analyzeSource(...)` and later `analysis.generateMasterManifest(...)` on `analysis-queue`.
- **CAS:** This process **polls `analysis-queue` only** and runs placeholder logic for local dev (replace with MinIO + `ffprobe` and real manifest stitching per `03-ANALYZER-WORKER-SPEC.md`).

Set **`temporal.register-stub-activity-workers=false`** on the orchestrator when CAS is deployed so analysis tasks are not double-handled.

## Configuration

| Property | Purpose |
|----------|---------|
| `temporal.enabled` | `false` skips Temporal (e.g. tests). Default: `true`. |
| `temporal.target` | Temporal frontend gRPC target. |
| `temporal.namespace` | Temporal namespace. |
| `server.port` | Default `8083` so CAS can run beside VES/orchestrator on one machine. |
| `cas.processing.enabled` | `true` (default): **MinIO** download/upload + **ffprobe** for `analyzeSource`, real `master.m3u8` upload for `generateMasterManifest`, HLS `#EXT-X-MEDIA` lines for `integrateSubtitles`. `false`: in-memory stubs (no MinIO/ffprobe; use in unit tests). Env: `CAS_PROCESSING_ENABLED`. |
| `minio.url` / `minio.access-key` / `minio.secret-key` | Required when `cas.processing.enabled=true`. |
| `app.media-processing.*` | Same keys as `temporal-orchestrator` processing mode: `hls-bucket`, `movies-key-prefix`, `temp-dir`, `ffprobe-path`. |
| `cas.complexity.enabled` | `true` (default): run **legacy heuristic CAS** + **ladder/cost** logic ported from `transcode-worker` after each `ffprobe` (logged and sent as Temporal activity heartbeat). `false`: no-op complexity (`ComplexityProfile.basic`). Env: `CAS_COMPLEXITY_ENABLED`. |

## Legacy parity (transcode-worker)

The following were **moved in spirit** from `media-content-platform/services/transcode-worker` into `bbmovie.transcode.cas.analysis` (same algorithms; Spring/JPA wiring removed):

- `HeuristicComplexityAnalysisService` → `HeuristicComplexityAnalysisService` + `ComplexityProfile` / `RecipeHints`
- `LadderGenerationService` → `CasLadderGenerationService` + `LadderRung`
- `ResolutionCostCalculator` → `ResolutionCostCalculator`

`analyzeSource` still returns the Temporal contract `MetadataDTO` only; ladder **hints** from CAS are not yet fed back into `VideoProcessingWorkflowImpl` (that workflow still plans rungs locally). Logs + heartbeats carry the legacy ladder for ops until the workflow consumes `RecipeHints`.

## Build

From `media-content-platform/services/transcode-services/java/`:

```text
mvn -pl transcode-contracts,cas -am verify
```
