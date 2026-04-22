---
name: Netflix Video Pipeline Plan
overview: Thiết kế lộ trình Netflix-style cho CAS/VVS/VQS theo hướng modular-first trong transcode-worker, đồng thời dựng hybrid data platform (Postgres + MinIO + analytics store) để sẵn sàng scale và tách microservice sau này.
todos:
  - id: schema-v1
    content: Design and version v1 data schema across Postgres, MinIO artifact metadata, and analytics sink contracts
    status: in_progress
  - id: vvs-mvp
    content: Implement and wire production VVS (ffprobe rule engine) into ExecutorStage/VideoProcessor with fail policy flags
    status: pending
  - id: vqs-mvp
    content: Implement and wire production VQS (VMAF-first with fallback metrics), plus persistence and analytics events
    status: pending
  - id: cas-upgrade
    content: Upgrade CAS from heuristic to quality-aware profile generation and feed RecipeHints into LGS
    status: pending
  - id: extraction-seams
    content: Introduce adapter boundaries and contracts to enable future service extraction without major refactor
    status: pending
isProject: false
---

# Netflix-Style CAS/VVS/VQS Implementation Plan

## Scope And Target
Triển khai kiến trúc giống Netflix theo 2 bước: (1) xây domain modules chuẩn trong `transcode-worker`, (2) tạo seam để tách thành microservices sau khi ổn định. Data layer dùng hybrid advanced: Postgres + MinIO + analytics store.

## Current Baseline To Reuse
- Có CAS-lite: probing + ladder + cost (`ProbeResult`, `LadderGenerationService`, `ResolutionCostCalculator`).
- Đã có phase-0 contracts/no-op cho CAS/VVS/VQS.
- Pipeline hiện tại: Fetch -> Probe -> Execute (`PipelineOrchestratorSetup`, `ProberStage`, `ExecutorStage`).

## Target Architecture (Modular-First, Netflix-Style)
```mermaid
flowchart LR
    fetchStage[FetcherStage] --> probeStage[ProberStage]
    probeStage --> casModule[CASModule]
    casModule --> lgsModule[LGSModule]
    lgsModule --> executeStage[ExecutorStage]
    executeStage --> vesModule[VESModule]
    vesModule --> vvsModule[VVSModule]
    vesModule --> vqsModule[VQSModule]
    vvsModule --> publishStatus[StatusPublisher]
    vqsModule --> publishStatus

    casModule --> postgresDb[PostgresMetadata]
    vvsModule --> postgresDb
    vqsModule --> postgresDb

    casModule --> minioArtifacts[MinIOArtifacts]
    vvsModule --> minioArtifacts
    vqsModule --> minioArtifacts

    postgresDb --> analyticsSink[AnalyticsStore]
```

## Data Platform Design
- Postgres:
  - transactional metadata + lifecycle states + queryable summaries.
  - tables: `analysis_job`, `complexity_profile`, `quality_report`, `validation_report`, `rendition_report`.
- MinIO:
  - raw artifacts (full ffprobe dump, VMAF JSON/log, validation evidence).
- Analytics store (ClickHouse or Elasticsearch):
  - denormalized time-series/events cho dashboard, trend, percentile latency, quality drift.
- Ingestion model:
  - write-through từ modules: Postgres + artifact URI, sau đó async publish event để sink sang analytics store.

## Phase Plan

### Phase 1: Solidify Module Contracts (no behavior break)
- Chuẩn hóa DTO + enums + persistence model versioning.
- Tạo package boundaries:
  - `service/complexity` (CAS)
  - `service/ladder` (LGS)
  - `service/validation/encode` (VVS)
  - `service/quality` (VQS)
- Add orchestration interfaces để `ExecutorStage` gọi VVS/VQS bằng policy, không hard-wire.

### Phase 2: VVS Production MVP
- Implement ffprobe-based validation rules:
  - codec/profile/level, width-height, bitrate range, audio presence/codec/channels/sample rate, duration tolerance.
- Persist validation summary vào Postgres, chi tiết vào MinIO.
- Policy by config: `fail-on-vvs` vs `warn-only`.

### Phase 3: VQS Production MVP
- Implement VMAF runner (`ffmpeg + libvmaf`) với fallback SSIM/PSNR.
- Sampling policy per ladder rung để kiểm soát cost.
- Persist score summary + artifact pointers; stream denormalized metrics sang analytics store.

### Phase 4: CAS Upgrade (from CAS-lite to CAS-real)
- Bổ sung feature extraction (motion, texture proxies, scene-change density).
- Add sample-encode + VQS feedback loop để sinh `RecipeHints` cho LGS.
- Cache/reuse complexity profile theo source hash + encode family.

### Phase 5: Extraction Seams For Future Microservices
- Tạo boundary adapters cho CAS/VVS/VQS:
  - in-process adapter (default)
  - remote client adapter (future microservice).
- Chuẩn hóa event contracts (NATS subjects + schema version).

## Code Areas To Change First
- Core orchestration:
  - [backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/pipeline/stage/ExecutorStage.java](backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/pipeline/stage/ExecutorStage.java)
  - [backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/processing/VideoProcessor.java](backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/processing/VideoProcessor.java)
- Module contracts and implementations:
  - [backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/validation/encode](backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/validation/encode)
  - [backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/quality](backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/quality)
  - [backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/complexity](backend/transcode-worker/src/main/java/com/bbmovie/transcodeworker/service/complexity)
- Config:
  - [backend/transcode-worker/src/main/resources/application.properties](backend/transcode-worker/src/main/resources/application.properties)

## Delivery Rules
- Feature-flag each module (`cas`, `vvs`, `vqs`) and rollout by environment.
- Idempotent writes keyed by `uploadId + rendition + analysisVersion`.
- Keep fast path bounded: VQS/CAS heavy tasks should be throttled by scheduler quotas.
- Add integration tests using small fixed media fixtures for deterministic assertions.

## Success Criteria
- VVS and VQS produce queryable reports + artifact links per rendition.
- CAS output influences LGS decisions beyond static height rules.
- System handles retries without duplicate logical reports.
- Modules can be switched from in-process to remote adapter without changing pipeline business flow.