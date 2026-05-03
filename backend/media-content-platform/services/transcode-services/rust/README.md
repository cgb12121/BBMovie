# BBMovie transcode — Rust workspace (Netflix-style boundaries)

This directory mirrors the **bounded-context split** used in the Java modules (`lgs`, `vis`, `ves`, `vvs`, `vqs`) and the workflow described in `../docs/02-WORKFLOW-AND-ACTIVITIES.md`: small, independently deployable workers around **shared contracts**, similar in spirit to Netflix’s pipeline of specialized encoding/validation services.

| Crate | Role | Java analogue |
|-------|------|----------------|
| `transcode-contracts` | Serde DTOs + Temporal queue name constants | `transcode-contracts` |
| `tc-ffprobe` | Subprocess `ffprobe -print_format json …` → `VideoMetadata` | bramp `FFprobe` + `MetadataService` |
| `tc-lgs` | Preset ladder + relative cost hints | LGS |
| `tc-runtime` | Shared runtime config + MinIO/S3 client + env parsing | Spring config + MinIO clients |
| `tc-vis` | Probe strategies (`PresignedUrl`, `PartialDownload`) with fallback order | VIS |
| `tc-ves` | Encoder worker binary (stub) | VES |
| `tc-vvs` | Playlist validation using MinIO + ffprobe | VVS |
| `tc-vqs` | MinIO-backed scoring with ffmpeg/libvmaf path | VQS |

Binaries are **thin** entrypoints; domain logic lives in libraries so each crate stays testable and dependency-light.

## Build

```bash
cd rust
cargo build --workspace
cargo test --workspace
```

## Runtime Environment

- `MINIO_API_URL` (default `http://localhost:9000`)
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` (default `minioadmin`)
- `MINIO_REGION` (default `us-east-1`)
- `HLS_BUCKET` (default `bbmovie-hls`)
- `SOURCE_BUCKET` (default `bbmovie-source`)
- `FFPROBE_PATH` (default `ffprobe`)
- `FFMPEG_PATH` (default `ffmpeg`)
- `TEMPORAL_ENABLED` (default `true`)
- `TEMPORAL_TARGET` / `TEMPORAL_NAMESPACE`
- `VVS_WORKER_REGISTER` (default `true`)
- `VQS_WORKER_REGISTER` (default `false`)
- `QUALITY_REFERENCE_TEMPLATE` (default `uploads/{upload_id}/source.mp4`)

## FFprobe and JSON (parity with Java)

- **Invocation** matches the usual ffprobe JSON dump: `-v error -hide_banner -print_format json -show_format -show_streams <input>`, where `<input>` is a file path or URL (same as Java `FFprobe.probe(String)`).
- **`VideoMetadata`** serializes like Java **`MetadataDTO`**: `width`, `height`, `durationSeconds`, `codec` (camelCase).
- **`QualityReport`** / **`ValidationRequest`** use camelCase for Jackson-compatible payloads (`renditionLabel`, `expectedWidth`, …).

### `vis` — probe + print metadata JSON

```bash
FFPROBE_PATH=ffprobe PROBE_BUCKET=bbmovie-source cargo run -p tc-vis --bin vis -- /path/to/file.mp4 uploads/demo/source.mp4
```

Stdout: pretty-printed `VideoMetadata`. Stderr: ladder lines (prefixed with `#`).

### `vvs` / `vqs` — playlist validation/scoring

```bash
FFPROBE_PATH=ffprobe cargo run -p tc-vvs --bin vvs -- /path/to/playlist.m3u8 examples/validation-request.json
USE_LIBVMAF=1 cargo run -p tc-vqs --bin vqs -- playlists/1080p.m3u8 examples/validation-request.json
```

For storage-backed mode, set:

```bash
USE_MINIO_DOWNLOAD=1
```

for `vvs`, and:

```bash
USE_LIBVMAF=1
```

for `vqs` (runs ffmpeg + libvmaf against reference + rendition).

Adjust `examples/validation-request.json` so `expectedWidth` / `expectedHeight` match the target rendition.

### Worker mode (queue registration runtime)

Each worker can run long-lived registration mode:

```bash
RUN_MODE=worker cargo run -p tc-vis --bin vis
RUN_MODE=worker VVS_WORKER_REGISTER=1 cargo run -p tc-vvs --bin vvs
RUN_MODE=worker VQS_WORKER_REGISTER=1 cargo run -p tc-vqs --bin vqs
```

This mode enforces VVS/VQS quality-queue ownership toggles similar to Java deployment policy.

### Optional real-media test

```bash
set BBMOVIE_FFPROBE_MEDIA=C:\path\to\clip.mp4
cargo test -p tc-ffprobe probe_real_media -- --ignored
```

## Parity Status

- `VIS`: strategy ordering + supports rules + presigned/partial fallback are implemented.
- `VVS`: MinIO playlist fetch + ffprobe dimension gate + failure codes implemented.
- `VQS`: MinIO fetch + ffprobe gate + ffmpeg/libvmaf scoring path implemented.
- `DTO contracts`: camelCase payloads aligned to Java contracts.
- `Temporal runtime`: queue registration/toggle runtime is present in worker mode (`RUN_MODE=worker`) using queue constants from `transcode-contracts`.
