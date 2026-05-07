# TEMP Plan: VQS Upgrade + CAS Subtitle Packaging Split

This is a temporary planning doc to align the next refactor wave for Java transcode services.

Scope now:
- Upgrade `vqs` from stub-like scoring to real quality scoring.
- Split subtitle packaging/integration out of `cas` into a dedicated subtitle service.

Out of scope now:
- Full `lgs` + `vis` unification (noted as hard and deferred with options below).

---

## 1) Current Problems

### 1.1 VQS is not yet "true quality scoring"
- `vqs` currently relies mainly on ffprobe dimension checks and a placeholder score.
- Responsibility boundary with `vvs` is blurry.

### 1.2 CAS is carrying packaging/subtitle concerns
- `cas` currently handles analysis plus manifest/subtitle integration.
- This mixes analysis policy concerns with packaging responsibilities.

### 1.3 VIS/LGS overlap remains
- `vis` and `lgs` both contain ladder/cost style logic.
- This is known hard refactor territory and should be staged after the above two changes.

---

## 2) Target Responsibility Model

### 2.1 VVS
- Strict technical validation only (resolution/codec/container/timing constraints).
- Return pass/fail + technical details.

### 2.2 VQS
- Perceptual quality scoring only (VMAF/libvmaf and quality metrics policy).
- Return quality score + score details.

### 2.5 Implemented Boundary (Hard Switch)
- VVS is now validation-only in the Java path:
  - no perceptual quality scoring semantics
  - deterministic validation reason codes
- VQS is now sole owner of perceptual quality scoring and quality gate decisions.
- Rollout mode: hard switch (no dual-path flag for VVS/VQS role boundary).

### 2.3 CAS
- Source analysis and policy hints only.
- No subtitle packaging/integration in final state.

### 2.4 New Subtitle Service (new worker)
- Subtitle normalization/translation/integration + master subtitle packaging.
- Own `integrateSubtitles` behavior and future subtitle-specific policies.

---

## 3) VQS Upgrade Plan (Phased)

## Phase A - Contract Clarification
- Keep `MediaActivities.validateAndScore` signature stable.
- Define `QualityReportDTO.detail` conventions for VQS:
  - `vqs_vmaf_pass`
  - `vqs_vmaf_fail`
  - `vqs_reference_missing`
  - `vqs_probe_error`
- Document threshold policy (example: pass when VMAF >= configurable threshold).

## Phase B - Real Scoring Implementation
- In `vqs`, fetch:
  - source/mezzanine reference
  - encoded rung playlist/media
- Run ffmpeg `libvmaf` comparison path.
- Parse score output and map to:
  - `passed`
  - `score`
  - `detail`
- Keep dimension sanity check as guardrail, not primary score source.

## Phase C - Config + Operability
- Add configs:
  - `vqs.vmaf.enabled`
  - `vqs.vmaf.model`
  - `vqs.vmaf.threshold`
  - `vqs.vmaf.threads`
  - `vqs.vmaf.timeout-seconds`
- Add structured logs:
  - uploadId, renditionLabel, vmafScore, elapsedMs, threshold.

## Phase D - Rollout
- Stage 1: shadow mode (compute score, do not fail workflow).
- Stage 2: soft enforcement (warn/fail only critical rungs).
- Stage 3: full enforcement policy.

---

## 4) CAS Subtitle Packaging Split Plan

## Phase A - Introduce dedicated subtitle worker/service
- Create new service (placeholder name: `sub`).
- Move implementation ownership of:
  - `normalizeSubtitle`
  - `translateSubtitle`
  - `integrateSubtitles`
- Keep API/contract stable to reduce orchestrator churn.

## Phase B - CAS cleanup
- In `cas`, deprecate/remove subtitle integration implementation.
- `cas` should only perform:
  - source probe/profile
  - complexity/risk/hints
  - analysis outputs for orchestration

## Phase C - Workflow routing update
- Ensure subtitle-related activities are routed to subtitle queue/worker only.
- Ensure `cas` worker rejects subtitle methods (fail-fast if invoked).

## Phase D - Data/manifest compatibility
- Preserve subtitle group behavior in master manifest:
  - single shared subtitle group id referenced by variants
- Add compatibility tests for old and new generated manifests.

---

## 5) VIS/LGS (Deferred but Planned)

This is difficult because VIS needs fast estimation while LGS is canonical ladder policy.

Recommended direction:
- Extract shared baseline ladder/cost logic into a shared module.
- Keep service-specific policy wrappers:
  - VIS: fast probe estimation usage
  - LGS: authoritative ladder resolution policy

Do not merge blindly in the same PR as VQS/CAS split.

---

## 6) Suggested Execution Order

1. VQS contract + implementation upgrade (behind flag).
2. Introduce subtitle service and route subtitle activities there.
3. Remove subtitle packaging from CAS.
4. Stabilize + observe metrics.
5. Start VIS/LGS shared-baseline extraction.

---

## 7) Acceptance Criteria

### VQS
- Quality score is produced by real scoring pipeline (not placeholder).
- Workflow decisions can use thresholded score policy.
- Clear telemetry exists per rung.

### CAS Split
- CAS no longer owns subtitle packaging logic.
- Subtitle service is sole owner of subtitle integration.
- Workflow still produces valid master + subtitle manifests.

### Safety
- No queue ambiguity for activity workers.
- Existing orchestrator contract remains backward compatible.

---

## 8) Reason Code Taxonomy (VVS vs VQS)

### VVS validation reason codes
- `vvs_validation_passed`
- `vvs_dimension_mismatch`
- `vvs_no_video_stream`
- `vvs_probe_error`

### VQS quality reason codes
- `quality_gate_passed`
- `quality_gate_failed`
- `dimension_mismatch`
- `libvmaf_failed`
- `libvmaf_timeout`
- `vmaf_parse_failed`
- `vqs_validation_failed`
