# VIS — Video Inspection Service

| Short name | **VIS** |
| Full name | **Video Inspection Service** |

Ported from `transcode-worker`: `FastProbeService`, `PresignedUrlProbeStrategy`, `PartialDownloadProbeStrategy`, `MetadataService` (as `VisMetadataService` + strategies). Ladder suffix and cost logic is now internal to VIS (`VisLadderGenerationService`) to keep service boundaries independent.

No Temporal task queue: use `VisFastProbeService` from Spring context or wire into CAS later. MinIO + `ffprobe` required when running probes.

## VIS v2 (Netflix-style staged upgrade)

- `VisProfileV2Service` now provides `SourceProfileV2` (rich source profile) while preserving existing `VisFastProbeService` behavior.
- Probe flow is now two-stage:
  - Fast probe (existing strategies)
  - Deep probe fallback when confidence/risk checks fail (`VisProbeDecisionPolicy`)
- Feature flags and rollout controls:
  - `vis.profile-v2.enabled` (default `true`)
  - `vis.profile-v2.analysis-version` (default `v2.0`)
  - `vis.profile-v2.min-duration-seconds-for-trust`
  - `vis.profile-v2.min-width-for-trust`
- Safe rollout recommendation:
  1. Run with `vis.profile-v2.enabled=true` in non-prod and inspect logs for deep-fallback rates.
  2. Validate confidence and gate reasons for diverse content classes.
  3. Keep consumers on legacy DTOs until downstream contract adoption is complete.
