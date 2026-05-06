# VQS — Video Quality Service

| Short name | **VQS** |
| Full name | **Video Quality Service** |

Alternate Spring Boot worker for the same Temporal **`quality-queue`** and **`validateAndScore`** contract as **VVS**, intended for **VMAF** / heavier quality scoring once wired. The current implementation mirrors the dimension check and uses a **stub score** when dimensions match (`detail` contains `vmaf_stub`).

## Configuration

- **`vqs.worker.register`**: default **`false`** in repo `application.properties` so VVS can own the queue. Set **`true`** only when this process is the sole registrar for `quality-queue`, and set **`vvs.worker.register=false`** on VVS in that deployment.
- **`temporal.enabled`**: same pattern as VVS for tests (`false`).

See `transcode-services/docs/05-QUALITY-WORKER-SPEC.md` for the mutual-exclusion policy.
