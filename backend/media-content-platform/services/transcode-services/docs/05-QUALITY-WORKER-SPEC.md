# Quality Activity Worker Specification (VVS/VQS)

The Quality Worker performs post-transcoding verification and scoring in the Temporal pipeline.

## 1. Role in the Workflow

After a resolution is encoded, the orchestrator calls `validateAndScore`. This is mapped to the `quality-queue`.

## 2. Responsibilities

### 2.1 Video Validation (VVS)
*   **Method:** Technical conformance check using `ffprobe`.
*   **Checks:** Verifies the file against `EncodingExpectations` (Correct codec, resolution, no frame drops).

### 2.2 Video Quality (VQS)
*   **Method:** Scores human-perceived quality using **VMAF**.
*   **Process:** Downloads both the original mezzanine and the encoded rung, then runs a comparison using FFmpeg `libvmaf`.

### 2.3 Reporting
*   Saves final metrics to the centralized Analysis DB (`tb_validation_report`, `tb_quality_report`).
*   Returns the quality score to the Temporal Workflow for final decision-making.

## 3. Distributed Considerations

*   **Compute:** VMAF is extremely CPU-heavy. Quality workers should scale independently from Encoder workers.
*   **Policy:** The Workflow can choose to skip VQS for low resolutions (e.g., 144p) to save compute costs.

## 4. Deployment: VVS vs VQS (single consumer on `quality-queue`)

Both Java workers register `MediaActivities` on the same Temporal task queue name (`quality-queue`). **Only one** of **VVS** or **VQS** should register in a given environment, or activity dispatch becomes ambiguous.

| Service | Property | Default (repo) | Notes |
|---------|-----------|----------------|--------|
| VVS (`bbmovie.transcode:vvs`) | `vvs.worker.register` | `true` | Primary validation path; `validateAndScore` uses ffprobe dimension checks (VMAF stub where noted in code). |
| VQS (`bbmovie.transcode:vqs`) | `vqs.worker.register` | `false` | Enable when running the quality-scoring worker; keep VVS registration off in the same deployment. |

Also gate Temporal client/worker beans with `temporal.enabled=false` for local/tests when no Temporal server is available.
