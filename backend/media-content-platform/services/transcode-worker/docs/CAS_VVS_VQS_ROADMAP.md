# CAS / VVS / VQS Roadmap for Transcode Worker

## 1) Objective
Build three video-analysis capabilities on top of the current `transcode-worker` pipeline:

- `CAS` (Complexity Analysis Service)
- `VVS` (Video Validation Service)
- `VQS` (Video Quality Service)

This document describes the current state, target architecture, and phased implementation plan.

---

## 2) Current State (What We Already Have)

## 2.1 CAS-lite (already present)
The current pipeline already contains an initial form of complexity analysis:

- `FastProbeService` + probe strategies (`PresignedUrlProbeStrategy`, `PartialDownloadProbeStrategy`) extract video metadata.
- `LadderGenerationService` generates an initial encoding ladder from source characteristics.
- `ResolutionCostCalculator` converts ladder resolutions into cost weights.
- `ProbeResult` stores `targetResolutions`, `peakCost`, `totalCost`.
- `ProberStage` uses probe cost to allocate scheduler resources early (`tryAcquire`).

This is a strong baseline, but it is still heuristic-based (height + static cost map), not quality-driven optimization.

## 2.2 VVS status
Current validation covers upload safety and file type:

- Content validation (`TikaValidationService`)
- Malware scanning (`ClamAVService`)

What is missing for VVS:

- Encoded output conformance validation (codec/profile/level/GOP/bitrate/audio/etc.).
- Validation against encode expectations per rendition.

## 2.3 VQS status
No quality scoring for encoded outputs yet:

- No VMAF computation.
- No PSNR/SSIM fallback metrics.
- No quality report persisted/published.

---

## 3) Target Service Boundaries

## 3.1 CAS (Complexity Analysis Service)
Responsibilities:

- Analyze source complexity and produce recipe/ladder hints.
- Optionally run sample encodes and evaluate quality/cost trade-off.
- Persist complexity profiles for reuse.

Input:

- Source metadata + optional source sample segments.

Output:

- `ComplexityProfile` (motion/texture/content class, score bins).
- `RecipeHints` (bitrate multiplier, CRF bounds, ladder pruning/special handling).

## 3.2 VVS (Video Validation Service)
Responsibilities:

- Validate each encoded rendition against expected media constraints.
- Return pass/fail + discrepancy details.

Input:

- Encoded file path/URL + expected constraints.

Output:

- `ValidationReport` with `PASS`, `WARN`, `FAIL` and structured violations.

## 3.3 VQS (Video Quality Service)
Responsibilities:

- Score encoded output quality against source (VMAF-first).
- Optionally compute PSNR/SSIM as fallback/secondary.

Input:

- Source path/URL + encoded rendition path/URL.

Output:

- `QualityReport` per rendition and aggregate quality summary.

---

## 4) Tooling and Runtime Dependencies

## 4.1 Required
- `ffprobe`: media stream/format inspection.
- `ffmpeg` with `libvmaf` support: VMAF execution.

## 4.2 Optional but useful
- PSNR/SSIM via ffmpeg filters.
- Hardware-specific tuning profiles (if quality checks become expensive at scale).

## 4.3 Startup capability checks
At application startup:

- Verify `ffmpeg` and `ffprobe` availability.
- Verify `libvmaf` support (`ffmpeg -filters` contains `libvmaf`).
- Expose capability in logs and health indicator.

---

## 5) Proposed Package Structure

Under `com.bbmovie.transcodeworker.service`:

- `complexity` (CAS)
  - `ComplexityAnalysisService`
  - `ComplexityProfileRepository` (phase 2+)
  - `dto` (profiles/hints)
- `validation` (VVS extension, keep existing upload validation classes)
  - `EncodeValidationService`
  - `rules` (codec/bitrate/gop/audio constraints)
  - `dto` (expectations/reports)
- `quality` (VQS)
  - `VideoQualityService`
  - `VmafService`
  - `dto` (scores/reports)

Note: Existing `service.validation` for Tika/ClamAV remains, but VVS logic should be clearly separated as encoded-output validation.

---

## 6) Delivery Plan (Phased)

## Phase 0 - Foundation (quick win, low risk)
Goal: Introduce contracts and hooks without changing production behavior.

Deliverables:

- Add DTO contracts:
  - `EncodingExpectations`
  - `ValidationReport`
  - `QualityReport`
  - `ComplexityProfile`
- Add service interfaces:
  - `ComplexityAnalysisService`
  - `EncodeValidationService`
  - `VideoQualityService`
- Add no-op/default implementations toggled by flags:
  - `app.analysis.cas.enabled=false`
  - `app.analysis.vvs.enabled=false`
  - `app.analysis.vqs.enabled=false`

Exit criteria:

- Build passes.
- Pipeline unchanged when all flags are disabled.

## Phase 1 - VVS MVP
Goal: Add encoded-output checks with strong ROI and predictable compute cost.

Deliverables:

- Implement `EncodeValidationService` using `ffprobe` output.
- Validate each rendition:
  - codec/container
  - width/height
  - avg bitrate bounds
  - audio presence/codec/sample rate/channels
  - duration tolerance
  - keyframe interval tolerance (if available in probe data)
- Run VVS immediately after each rendition encode and before final status publish.

Exit criteria:

- Failed constraints produce structured `ValidationReport` and mark task failed (or warn-only by config).

## Phase 2 - VQS MVP (VMAF-first)
Goal: Produce objective quality scores for encoded renditions.

Deliverables:

- Implement `VmafService` via ffmpeg `libvmaf`.
- VQS policy:
  - Run on selected renditions first (for example `1080p`, `720p`, `480p`) to control cost.
  - Optional downscaled source comparison for deterministic dimensions.
- Persist/publish quality reports (NATS or DB integration point).

Exit criteria:

- `QualityReport` generated per selected rendition.
- Failure policy configurable (`warn-only` vs hard fail).

## Phase 3 - CAS upgrade (from CAS-lite to quality-aware)
Goal: Replace static heuristics with adaptive complexity-driven hints.

Deliverables:

- Add lightweight complexity features:
  - motion/scene-change indicators
  - spatial detail proxies
  - duration buckets
- Run sample-encode + VQS loop on representative chunks.
- Produce `RecipeHints` consumed by `LadderGenerationService`.
- Persist `ComplexityProfile` per asset family to avoid recompute.

Exit criteria:

- Ladder/recipe selection can diverge per content class (not only source height).
- Evidence of bitrate/quality efficiency improvements.

---

## 7) Pipeline Integration Points

Current high-level pipeline:

`Fetch -> Probe -> Execute`

Recommended integration:

- `Probe` stage:
  - CAS-lite already active.
  - Phase 3: call upgraded CAS here and enrich `ProbeResult`.
- `Execute` stage:
  - Encode renditions.
  - Run VVS per rendition.
  - Run VQS per policy.
  - Publish status + optional analysis report references.

---

## 8) Non-Functional Requirements

- Resource safety:
  - VQS is compute-heavy; throttle with scheduler-aware quotas.
- Idempotency:
  - Re-delivered tasks should not duplicate report records.
- Observability:
  - Metrics for VVS failure reasons, VMAF latency/cost, CAS cache hit ratio.
- Feature flags:
  - Must be togglable per environment.

---

## 9) Suggested Immediate Next Actions

1. Implement Phase 0 contracts/interfaces and config flags.
2. Implement Phase 1 VVS MVP with `ffprobe` rule checks.
3. Add a small golden test set (3-5 clips) for regression in validation and quality flow.
4. Confirm `ffmpeg` build in deployment environment includes `libvmaf` before enabling Phase 2.

