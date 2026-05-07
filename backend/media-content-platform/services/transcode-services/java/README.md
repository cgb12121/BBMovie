# Transcode Java Cluster - Setup & Run Guide

This folder contains the Java transcode cluster modules:

- `transcode-contracts` (shared DTOs and activity contracts)
- `temporal-orchestrator` (workflow orchestration + tracking API)
- `cas` (analysis queue worker)
- `ves` (encoding queue worker)
- `vvs` (validation worker on `validation-queue`)
- `vqs` (quality scoring worker on `quality-queue`)
- `lgs`, `vis` (supporting services and logic modules)

## 1) Prerequisites

- Java 21+ (project currently builds with newer JDK as well)
- Maven 3.9+
- Docker Desktop (for Temporal/MinIO/NATS and dependencies)
- `ffmpeg` + `ffprobe` available on PATH for media workers
- Optional for VQS quality mode: FFmpeg built with `libvmaf`

## 2) Start infrastructure

From repo `backend` root:

```powershell
docker compose up -d minio minio-mc nats temporal-postgresql temporal temporal-ui
```

Useful endpoints:

- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Temporal frontend: `localhost:7233`
- Temporal UI: `http://localhost:7088`
- NATS: `nats://localhost:4222`

## 3) Build all Java modules once

From this `java` folder:

```powershell
mvn clean install -DskipTests
```

## 4) Run services (recommended local order)

Open separate terminals, all from this `java` folder.

1. CAS worker (analysis queue):

```powershell
mvn -pl cas spring-boot:run
```

2. VES worker (encoding queue):

```powershell
mvn -pl ves spring-boot:run
```

3. VVS worker (validation gate on `validation-queue`):

```powershell
mvn -pl vvs spring-boot:run
```

4. VQS worker (perceptual quality gate on `quality-queue`, typically `libvmaf`):

```powershell
mvn -pl vqs spring-boot:run
```

5. Temporal Orchestrator:

```powershell
mvn -pl temporal-orchestrator spring-boot:run
```

### Windows `.bat` shortcuts (added)

- Per-service local scripts:
  - `cas/run.bat`
  - `ves/run.bat`
  - `lgs/run.bat`
  - `vis/run.bat`
  - `vvs/run.bat`
  - `vqs/run.bat`
  - `temporal-orchestrator/run.bat`
- Run whole cluster from `java` folder:
  - `build-contracts.bat` (build `transcode-contracts` once before launching services)
  - `run-cluster.bat` (starts full cluster with both VVS + VQS)
  - `launcher.bat` (interactive menu: run single service or full cluster)

Each service script loads `.env` in its own folder (if present), then parent `java/.env` (if present), then runs `mvn spring-boot:run -DskipTests`.

## 5) Environment variables you usually need

Common:

- `TEMPORAL_TARGET=localhost:7233`
- `TEMPORAL_NAMESPACE=default`
- `MINIO_API_URL=http://localhost:9000`
- `MINIO_ACCESS_KEY=minioadmin`
- `MINIO_SECRET_KEY=minioadmin`
- `FFMPEG_PATH=ffmpeg`
- `FFPROBE_PATH=ffprobe`

Orchestrator bridge (optional auto-start from MinIO/NATS events):

- `NATS_BRIDGE_ENABLED=true`
- `NATS_URL=nats://localhost:4222`
- `NATS_MINIO_SUBJECT=minio.events`
- `NATS_STREAM_NAME=BBMOVIE`

VQS (if enabled):

- `VMAF_ENABLED=true`
- `VMAF_PASS_THRESHOLD_MEAN=93.0`
- `VMAF_PASS_THRESHOLD_P10=88.0`
- `VMAF_PASS_THRESHOLD_WORST_WINDOW=85.0`

## 6) Verify cluster health

Actuator endpoints (examples):

- Orchestrator: `http://localhost:8501/actuator/health`
- CAS: `http://localhost:8503/actuator/health`
- LGS: `http://localhost:8504/actuator/health`
- VIS: `http://localhost:8505/actuator/health`
- VES: `http://localhost:8506/actuator/health`
- VVS: `http://localhost:8507/actuator/health`
- VQS: `http://localhost:8508/actuator/health`

## 7) Tracking API

The orchestrator exposes workflow tracking:

```http
GET /api/transcode/{uploadId}/status
```

Example:

```powershell
curl http://localhost:8501/api/transcode/<uploadId>/status
```

Response includes workflow phase, lifecycle status, timeline, and quality metrics fields (when available).

## 8) Troubleshooting

- `Failed to connect Temporal`: check `temporal` container and `TEMPORAL_TARGET`.
- `NATS connection refused`: ensure `nats` container is up and `NATS_URL` is correct.
- `ffprobe/ffmpeg not found`: set `FFPROBE_PATH` / `FFMPEG_PATH` explicitly.
- Validation or quality worker not receiving tasks: verify VVS is on `validation-queue` and VQS is on `quality-queue`.
- MinIO access errors: validate bucket setup from `minio-mc` init and credentials.

