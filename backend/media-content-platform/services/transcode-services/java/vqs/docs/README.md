# VQS — Video Quality Service

| Short name | **VQS** |
| Full name | **Video Quality Service** |

Spring Boot worker for Temporal **`quality-queue`** and **`validateAndScore`** as the perceptual quality gate (VMAF-driven scoring).

## Configuration

- **`vqs.worker.register`**: default **`true`** in repo `application.properties`, registers worker on `quality-queue`.
- **`temporal.enabled`**: same pattern as VVS for tests (`false`).

VQS is expected to run in parallel with VVS: VVS handles validation on `validation-queue`, VQS handles perceptual quality on `quality-queue`.
