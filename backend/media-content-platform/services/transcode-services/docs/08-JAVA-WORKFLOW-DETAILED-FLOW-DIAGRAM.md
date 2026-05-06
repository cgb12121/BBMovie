# Java Transcode Workflow Detailed Flow (Current)

## Scope

This document describes the current Java workflow path from `temporal-orchestrator` to workers, including the updated fan-out/fan-in behavior and queue boundaries.

## High-level execution flow

```mermaid
flowchart TD
    U[Upload event / start request] --> W[VideoProcessingWorkflowImpl.processUpload]
    W --> A1[analyzeSource on analysis-queue]
    A1 --> P[planRungs from source metadata]

    P --> E1[Fan-out encodeResolution per rung]
    E1 --> Q1[Per-rung quality check pipeline for eligible rungs]
    E1 --> E2[Fan-in all encode promises]
    Q1 --> Q2[Fan-in all quality promises]

    E2 --> M[generateMasterManifest on analysis-queue]
    Q2 --> M
    M --> D[Workflow done]
```

## Queue and worker boundaries

```mermaid
flowchart LR
    subgraph Orchestrator["temporal-orchestrator (workflow worker)"]
        WF[VideoProcessingWorkflowImpl]
    end

    subgraph Analysis["analysis-queue workers"]
        CAS[CAS/LGS or analysis activity impl]
    end

    subgraph Encoding["encoding-queue workers"]
        VES[VES: VesMediaActivities -> VesEncodingProcessingService]
    end

    subgraph Quality["quality-queue workers"]
        VVS[VVS or VQS quality activity impl]
    end

    WF -->|analyzeSource| CAS
    WF -->|encodeResolution fan-out| VES
    WF -->|validateAndScore fan-out| VVS
    WF -->|generateMasterManifest| CAS
```

## Detailed sequence (fan-out/fan-in)

```mermaid
sequenceDiagram
    participant ORC as temporal-orchestrator workflow
    participant ANA as analysis-queue worker
    participant ENC as encoding-queue (VES)
    participant QLY as quality-queue (VVS/VQS)

    ORC->>ANA: analyzeSource(uploadId, bucket, key)
    ANA-->>ORC: MetadataDTO
    ORC->>ORC: planRungs(metadata.height)

    loop Each planned rung
        ORC->>ENC: Async encodeResolution(EncodeRequest)
        note right of ORC: Promise<RungResultDTO> created
        opt rung height >= 720
            ORC->>ORC: attach quality pipeline on encode promise
            ENC-->>ORC: RungResultDTO
            alt encode success
                ORC->>QLY: Async validateAndScore(ValidationRequest)
            else encode failed
                ORC-->>ORC: synthetic failed quality result
            end
        end
    end

    ORC->>ORC: fan-in encode Promise.allOf(...)
    ORC->>ORC: collect rung results
    ORC->>ORC: fan-in quality Promise.allOf(...)
    ORC->>ORC: enforce passed() gate

    ORC->>ANA: generateMasterManifest(rungResults)
    ANA-->>ORC: FinalManifestDTO
```

## VES internal flow (encode activity)

```mermaid
flowchart TD
    S[encodeResolution request] --> URL[Generate presigned GET URL for source object]
    URL --> TMP[Create per-rung temp output dir]
    TMP --> FF[Run FFmpeg HLS transcode from URL input]
    FF --> HB1[Heartbeat on FFmpeg progress]
    HB1 --> UP[Parallel upload HLS files to MinIO]
    UP --> HB2[Heartbeat on upload progress]
    HB2 --> CLN[Cleanup rung temp dir]
    CLN --> OUT[Return RungResultDTO]
```

## Concurrency model summary

- Workflow fan-out happens in `VideoProcessingWorkflowImpl` using `Async.function(...)`.
- Worker-level concurrency is controlled in `VesWorkerLifecycle` with `WorkerOptions`:
  - `maxConcurrentActivityExecutionSize`
  - `maxConcurrentActivityTaskPollers`
- Per-task CPU pressure is controlled by `app.media-processing.ffmpeg-threads`.
- Upload IO parallelism is controlled by `app.media-processing.upload-parallelism`.

## Notes

- The workflow is deterministic and lightweight by design.
- Actual throughput depends on worker replica count, queue lag, CPU headroom, and MinIO throughput.
- Quality gate currently applies to eligible rungs (>=720p) and fails workflow when a required quality check fails.
