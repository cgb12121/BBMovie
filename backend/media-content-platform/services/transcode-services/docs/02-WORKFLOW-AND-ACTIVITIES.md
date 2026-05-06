# Temporal Workflow and Activity Definitions

This document defines the "contracts" (Interfaces) between the Temporal Orchestrator and the Activity Workers.

## Java module
Shared types live in **`transcode-contracts`** (`bbmovie.transcode.contracts`): `MediaActivities`, DTOs in `bbmovie.transcode.contracts.dto`, and queue name constants in `bbmovie.transcode.contracts.temporal.TemporalTaskQueues`. The orchestrator and worker services (VES, CAS, VVS, …) depend on this jar so activity names and payloads stay aligned.

## 1. VideoProcessingWorkflow

This is the main state machine that coordinates the life of a single video upload.

### Workflow Logic
1.  **Analyze:** Call `AnalyzeActivity`.
2.  **Plan:** (Local logic) Decide which resolutions are needed based on source height.
3.  **Fan-out:** Start $N$ `EncodeActivity` instances in parallel.
4.  **Barrier:** Wait for all `EncodeActivity` instances to complete.
5.  **Validate:** Call `ValidateAndScoreActivity` for the high-quality renditions.
6.  **Finalize:** Call `ManifestGeneratorActivity` to create the `master.m3u8`.

## 2. Activity Interface

### 2.1 MediaActivities
Workers implementing this interface are distributed across the cluster.

| Method | Input | Output | Queue |
| :--- | :--- | :--- | :--- |
| `analyzeSource` | `uploadId`, `bucket`, `key` | `MetadataDTO` | `analysis-queue` |
| `encodeResolution` | `EncodeRequest` | `RungResultDTO` | `encoding-queue` |
| `validateAndScore` | `ValidationRequest` | `QualityReportDTO` | `quality-queue` |
| `generateMasterManifest` | `List<RungResult>` | `FinalManifestDTO` | `analysis-queue` |
| `normalizeSubtitle` | `uploadId`, `bucket`, `key` | `SubtitleJsonDTO` | `subtitle-queue` |
| `translateSubtitle` | `SubtitleJsonDTO`, `targetLang`| `SubtitleJsonDTO` | `subtitle-queue` |
| `integrateSubtitles` | `uploadId`, `List<SubInfo>` | `ManifestUpdateDTO` | `analysis-queue` |

## 3. Distributed Data Contracts (DTOs)

### EncodeRequest
```json
{
  "uploadId": "string",
  "resolution": "1080p",
  "width": 1920,
  "height": 1080,
  "masterKey": "hex-string",
  "masterIV": "hex-string"
}
```

### RungResultDTO
```json
{
  "resolution": "1080p",
  "playlistPath": "bbmovie-hls/movies/{id}/1080p/playlist.m3u8",
  "success": true
}
```

## 4. Retry and Timeout Policies

### Encoder Workers
*   **Start-to-Close Timeout:** 6 hours (Large files take time).
*   **Heartbeat Timeout:** 2 minutes (Detect if a worker node crashes mid-encode).
*   **Retry Policy:** 3 attempts. Backoff coefficient: 2.0.

### Analyzer Workers
*   **Start-to-Close Timeout:** 5 minutes.
*   **Retry Policy:** 5 attempts (Fast retries for network glitches).
