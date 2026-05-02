# Analyzer Activity Worker Specification

## Service name (canonical)

| Short name | Full name | Role |
|------------|-----------|------|
| **CAS** | **Complex Analysis Service** | Temporal activity worker on **`analysis-queue`**: `analyzeSource`, `generateMasterManifest`, `integrateSubtitles` (see `docs/02-WORKFLOW-AND-ACTIVITIES.md`). |

Spring Boot module: `media-content-platform/services/transcode-services/java/cas` (`spring.application.name=cas`). Operator docs: `cas/docs/README.md`.

The spec below also refers to **VIS** (video inspection / `ffprobe`) and **complexity analysis** as logical sub-steps; **CAS** is the current deployable worker for the analysis task queue.

---

The Analyzer Worker implements the pre-transcoding logic within the Temporal pipeline.

## 1. Role in the Workflow

The analyzer is the first activity called by the `VideoProcessingWorkflow`. It is mapped to the `analysis-queue`.

## 2. Responsibilities

### 2.1 Video Inspection (VIS)
*   **Activity:** `analyzeSource`
*   **Method:** Probes the mezzanine file using `ffprobe` via Presigned URLs.
*   **Goal:** Return metadata (resolution, duration, codec) to the workflow state.

### 2.2 Complexity Analysis (CAS)
*   Calculates a complexity score based on heuristics (dimensions/bitrate/codec).
*   Determines the "Content Class" (e.g., animation vs high-action sports).
*   **Implementation note:** The `java/cas` service performs **VIS**-style `ffprobe` in `analyzeSource` (returns `MetadataDTO`). It also runs **legacy CAS + ladder + cost** logic ported from `transcode-worker` (`HeuristicComplexityAnalysisService`, `LadderGenerationService`, `ResolutionCostCalculator`) under `bbmovie.transcode.cas.analysis` — results are logged and heartbeated; the Temporal workflow still plans rungs locally until `MetadataDTO` or workflow inputs are extended to carry `RecipeHints`.

### 2.3 Manifest Generation
*   **Activity:** `generateMasterManifest`
*   **Method:** Once all parallel encoding activities finish, this worker is called to stitch the resolution playlists into one `master.m3u8`.

## 3. Distributed Considerations

*   **Concurrency:** Since VIS is mostly network-bound, a single worker instance can handle many concurrent `analyzeSource` tasks.
*   **No State:** The worker is completely stateless. The "Ladder" and "Metadata" are passed back to Temporal, which persists them.
