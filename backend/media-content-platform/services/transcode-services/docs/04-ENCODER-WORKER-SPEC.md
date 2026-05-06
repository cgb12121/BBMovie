# Encoder Activity Worker Specification (VES)

## Service name (canonical)

| Short name | Full name | Role |
|------------|-----------|------|
| **VES** | **Video Encoding Service** (also described as the **video encoding / transcoding** worker) | Temporal activity worker on **`encoding-queue`**; runs FFmpeg (or equivalent) to produce HLS renditions per rung. |

Spring Boot module in this repo: `media-content-platform/services/transcode-services/java/ves` (`spring.application.name=ves`). Operator docs: `ves/docs/README.md`.

The Encoder Worker (VES) performs the high-compute transcoding work in the Temporal pipeline.

## 1. Role in the Workflow

The orchestrator invokes the `encodeResolution` activity in **parallel** for every rung in the bitrate ladder. These activities are mapped to the `encoding-queue`.

## 2. Responsibilities

### 2.1 Video Transcoding
*   **Input:** `EncodeRequest` (Resolution, MasterKey, uploadId).
*   **Process:**
    1. Downloads the mezzanine file from MinIO.
    2. Runs FFmpeg with the specific target resolution.
    3. Generates HLS segments and a resolution-level playlist (`.m3u8`).
    4. Derived encryption keys are created locally using the `MasterKey`.
    5. Uploads segments to `bbmovie-hls` and keys to `bbmovie-secure`.

### 2.2 Heartbeating (Critical)
*   Transcoding can take hours. The worker MUST call `Activity.getExecutionContext().heartbeat(details)` every 30 seconds.
*   If Temporal doesn't receive a heartbeat within the timeout, it assumes the node crashed and restarts the activity on another node.

### 2.3 Resource Control
*   Uses a local `TranscodeScheduler` (Semaphore) to limit concurrent FFmpeg processes based on the node's CPU cores.
*   **Security:** Runs FFmpeg inside a restricted container sandbox (No network access).

## 3. Distributed Considerations

*   **Node Affinity:** Encoder workers should be deployed on high-CPU/Compute-optimized instances.
*   **Cleaning:** The worker MUST clean up the `TEMP_DIR` after every activity, regardless of success or failure.
