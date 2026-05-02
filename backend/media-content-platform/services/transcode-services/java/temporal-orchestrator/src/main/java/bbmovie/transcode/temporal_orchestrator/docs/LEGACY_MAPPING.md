# Legacy NATS worker to Temporal orchestrator mapping

Legacy service: `media-content-platform/services/transcode-worker` (`com.bbmovie.transcodeworker`).

## Trigger path

| Legacy | Temporal orchestrator |
|--------|------------------------|
| MinIO publishes to JetStream subject (e.g. `minio.events`). | Same. |
| `FetcherStage` pulls messages, parses S3 event metadata (`purpose`, `uploadId`), optional MinIO stat for size/content-type. | `MinioEventParser` + `TranscodeWorkflowNatsBridge` perform the same parsing (without MinIO HEAD; size comes from event JSON when present). |
| Message flows to `ProberStage` then `ExecutorStage`. | After a successful `WorkflowClient.start`, the JetStream message is **ACK**’d (with `inProgress()` heartbeats while starting). |
| `ExecutorStage` runs `MediaProcessor` (video/image). | `VideoProcessingWorkflowImpl` runs the video pipeline only for `MOVIE_SOURCE` / `MOVIE_TRAILER`; other purposes complete the workflow immediately (image handling remains for a follow-up workflow or legacy worker). |

## Video processing steps

| Legacy (`VideoProcessor` and pipeline) | Temporal (this module) |
|----------------------------------------|---------------------------|
| Fast probe + scheduler sizing in `ProberStage` / `ExecutorStage`. | `MediaActivities.analyzeSource` on `analysis-queue` (`MetadataDTO`); ladder planning is workflow-local from source height. |
| `ComplexityAnalysisService` + `LadderGenerationService` + FFmpeg transcode all rungs in one JVM. | Planned rungs → parallel `MediaActivities.encodeResolution` on `encoding-queue` (one activity per rung). |
| Post-encode VVS/VQS per rendition in-process. | `MediaActivities.validateAndScore` on `quality-queue` for 720p+ rungs in the workflow implementation. |
| Upload HLS + master playlist. | Stub uses paths under `bbmovie-hls/movies/{uploadId}/...`; real workers should perform uploads and return `RungResultDTO` / `FinalManifestDTO` as in `media-content-platform/services/transcode-services/docs/02-WORKFLOW-AND-ACTIVITIES.md`. |
| `StatusPublisher` → NATS `media.status.update`. | Not implemented here; add a dedicated activity or signal when upstream services must stay unchanged. |

## Activity contract

The Java interface `bbmovie.transcode.contracts.activity.MediaActivities` (artifact `transcode-contracts`) matches the logical contract in `media-content-platform/services/transcode-services/docs/02-WORKFLOW-AND-ACTIVITIES.md`. The workflow uses **the same interface** with **different `ActivityOptions.setTaskQueue(...)`** stubs so each method is routed to the queue listed in that document. The **CAS** worker (`java/cas`) implements `analyzeSource`, `generateMasterManifest`, and `integrateSubtitles` on `analysis-queue` (`cas/docs/README.md`). The **VES** worker (`java/ves`) implements `encodeResolution` on `encoding-queue` (`ves/docs/README.md`).

## Subtitle pipeline

`normalizeSubtitle`, `translateSubtitle`, and `integrateSubtitles` are on the interface for parity with the doc; the current `VideoProcessingWorkflowImpl` does not call them (optional follow-up when subtitle workers exist).
