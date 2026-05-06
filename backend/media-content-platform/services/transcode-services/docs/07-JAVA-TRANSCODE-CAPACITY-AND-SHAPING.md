# Java Transcode Capacity and Shaping Guide

## Goal

This guide maps a Netflix-style "resource shaping + bin packing" mindset to the current Java Temporal stack in this repository.

It is not about copying Titus internals. It is about operating `temporal-orchestrator` + workers (`cas`, `vis`, `ves`, `vvs`/`vqs`) so one cluster can run many transcode jobs concurrently without CPU/IO collapse.

## Core operating model

- Treat each encode activity as one schedulable unit.
- Shape each worker by CPU, memory, and disk profile.
- Keep queue isolation by workload class:
  - `analysis-queue` (light/medium CPU, probe/plan work)
  - `encoding-queue` (heavy CPU + IO + temp disk)
  - `quality-queue` (medium CPU, ffprobe/vmaf path)
  - `subtitle-queue` (light CPU)
- Scale horizontally by worker replicas, not by making one process own all jobs.

## Recommended shaping baseline

Use these as starting points, then tune from real metrics.

| Worker | Profile | Suggested shape |
|---|---|---|
| `cas` / `vis` | Analysis/probe | 1-2 vCPU, 2-4 GB RAM |
| `ves` | Encoding | 8-16 vCPU, 16-32 GB RAM, fast local SSD |
| `vvs` / `vqs` | Quality | 2-4 vCPU, 4-8 GB RAM |

## VES concurrency knobs

Current VES supports these controls:

- `temporal.max-concurrent-activity-executions`
- `temporal.max-concurrent-activity-task-pollers`
- `app.media-processing.ffmpeg-threads`
- `app.media-processing.upload-parallelism`

### Practical sizing formula

For one `ves` node:

`total_encode_cpu_threads = maxConcurrentActivityExecutions * ffmpegThreads`

Target this close to available CPU threads (not far above).  
If node has 16 vCPU, a safe starting point is:

- `max-concurrent-activity-executions=8`
- `ffmpeg-threads=1`

Then increase only after observing sustained headroom.

## Throughput tuning sequence

Tune in this order:

1. Keep workflow fan-out/fan-in behavior enabled.
2. Raise worker replicas for `encoding-queue`.
3. Adjust per-node `maxConcurrentActivityExecutions`.
4. Tune `ffmpegThreads` per task.
5. Tune `uploadParallelism` for segment upload.

Do not increase all knobs at once.

## What to monitor

At minimum, capture these per queue and per worker:

- queue backlog / schedule-to-start latency
- activity execution duration (`encodeResolution`, `validateAndScore`)
- worker CPU saturation
- disk usage and temp/cache directory growth
- MinIO request error rate and throughput
- retry volume and timeout rate

## Failure control policy

- Keep heartbeat for long-running encode activities.
- Mark non-retryable business failures explicitly where appropriate (for unrecoverable media errors).
- Keep temp/cache cleanup policy (startup or scheduled TTL cleanup).

## Deployment patterns

### Pattern A: Single class VES pool

- All encode jobs share one `encoding-queue`.
- Simpler operation, good first production baseline.

### Pattern B: Multi-class encode pools

- Split by profile (`encoding-hd`, `encoding-sd`, etc.) when throughput variance is high.
- Route workflow requests by rung class.

Pattern B is more complex but helps avoid head-of-line blocking from heavy rungs.

## Checklist for production cutover

- `temporal-orchestrator` health and queue routing verified.
- VES worker options applied from config and visible in startup logs.
- Heartbeat observed for long encode runs.
- Cache/temp disk housekeeping enabled.
- Load test done with multiple concurrent uploads and mixed durations.
- Autoscaling thresholds defined from queue lag + CPU.
