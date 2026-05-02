# BBMovie transcode — Rust workspace (Netflix-style boundaries)

This directory mirrors the **bounded-context split** used in the Java modules (`lgs`, `vis`, `ves`, `vvs`, `vqs`) and the workflow described in `../docs/02-WORKFLOW-AND-ACTIVITIES.md`: small, independently deployable workers around **shared contracts**, similar in spirit to Netflix’s pipeline of specialized encoding/validation services.

| Crate | Role | Java analogue |
|-------|------|----------------|
| `transcode-contracts` | Serde DTOs + Temporal queue name constants | `transcode-contracts` |
| `tc-ffprobe` | Subprocess `ffprobe -print_format json …` → `VideoMetadata` | bramp `FFprobe` + `MetadataService` |
| `tc-lgs` | Preset ladder + relative cost hints | LGS |
| `tc-vis` | Probe strategies (`FfprobeProbe`, `StubProbe`) | VIS |
| `tc-ves` | Encoder worker binary (stub) | VES |
| `tc-vvs` | Playlist ffprobe + dimension gate | VVS |
| `tc-vqs` | Playlist ffprobe + stub VMAF score | VQS |

Binaries are **thin** entrypoints; domain logic lives in libraries so each crate stays testable and dependency-light.

## Build

```bash
cd rust
cargo build --workspace
cargo test --workspace
```

## FFprobe and JSON (parity with Java)

- **Invocation** matches the usual ffprobe JSON dump: `-v error -hide_banner -print_format json -show_format -show_streams <input>`, where `<input>` is a file path or URL (same as Java `FFprobe.probe(String)`).
- **`VideoMetadata`** serializes like Java **`MetadataDTO`**: `width`, `height`, `durationSeconds`, `codec` (camelCase).
- **`QualityReport`** / **`ValidationRequest`** use camelCase for Jackson-compatible payloads (`renditionLabel`, `expectedWidth`, …).

### `vis` — probe + print metadata JSON

```bash
FFPROBE_PATH=ffprobe cargo run -p tc-vis --bin vis -- /path/to/file.mp4
```

Stdout: pretty-printed `VideoMetadata`. Stderr: ladder lines (prefixed with `#`).

### `vvs` / `vqs` — probe HLS playlist + print quality JSON

```bash
FFPROBE_PATH=ffprobe cargo run -p tc-vvs --bin vvs -- /path/to/playlist.m3u8 examples/validation-request.json
```

Adjust `examples/validation-request.json` so `expectedWidth` / `expectedHeight` match the probed rendition.

### Optional real-media test

```bash
set BBMOVIE_FFPROBE_MEDIA=C:\path\to\clip.mp4
cargo test -p tc-ffprobe probe_real_media -- --ignored
```

## Next steps

- Wire **Temporal Rust SDK** (or gRPC) using `transcode_contracts::temporal` queue names.
- MinIO presigned URLs for `vis` / workers (same as Java).
- Keep **one** registrant on `quality-queue` per deployment (VVS vs VQS), matching `../docs/05-QUALITY-WORKER-SPEC.md`.
