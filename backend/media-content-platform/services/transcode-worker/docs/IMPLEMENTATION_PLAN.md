# Transcode Worker Implementation Plan

## 🎯 Objective
Enhance the Transcode Worker to support **Parallel Processing** with **Resource Management**, while preserving existing stability fixes (robust cleanup, resilience, and security).

---

## 🏗️ Architecture Components

### 1. `TranscodeScheduler` (New)
*   **Role**: Manages CPU/Resource capacity to prevent server overload.
*   **Mechanism**: Weighted Semaphore.
*   **Config**: `app.transcode.max-capacity` (default: 16).
*   **Logic**:
    *   Acquire permits based on resolution cost.
    *   Release permits in `finally` block.

### 2. `VideoTranscoderService` (Enhancement)
*   **Parallelism**: Use `CompletableFuture` to run resolutions in parallel (async).
*   **Scheduler Integration**: Wrap transcoding jobs with `scheduler.runWithResourceConstraint()`.
*   **Cost Calculation**:
    *   1080p: 8 points
    *   720p: 4 points
    *   480p: 2 points
    *   Others: 1 point
*   **Encryption**: Ensure **Manual Static Key** generation logic (writing `enc.key` to disk) is preserved and thread-safe.

---

## 🛡️ Preserved Bug Fixes (Do Not Regression)

1.  **Blocking Consumer**: `MediaEventConsumer` MUST use `Executors.newVirtualThreadPerTaskExecutor()` to offload `processRecord`.
2.  **Robust Cleanup**: `MediaEventConsumer` MUST use `try-finally` with `FileUtils.deleteDirectory` to clean up temp folders.
3.  **Tika/ClamAV Resilience**:
    *   `ClamAVService` must respect `app.clamav.enabled` and profile-based bypass.
    *   `TikaValidationService` must catch `Throwable` (or `Exception`) to handle native/dependency errors gracefully.
4.  **Path Safety**: Temp files must use safe names (`source.mp4`), not original user filenames.
5.  **Admin Management**: Deletion logic cascading to HLS/Key buckets must be preserved.

---

## 📝 Step-by-Step Implementation Guide

### Step 1: Scheduler Implementation
1.  Create `com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler`.
2.  Implement `Semaphore` logic.

### Step 2: Service Refactoring
1.  Modify `VideoTranscoderService`:
    *   Inject `TranscodeScheduler`.
    *   Add `getCostWeight()` to `VideoResolution`.
    *   Refactor `transcode()` to use `CompletableFuture.runAsync(...)` stream.
    *   **Crucial**: Ensure `ffmpegExecutor` is thread-safe or created per task (created per task is safer for now).

### Step 3: Configuration
1.  Add `app.transcode.max-capacity=16` to `application.properties`.

### Step 4: Verification
1.  Start app.
2.  Upload multiple videos (or one video triggering multiple resolution tasks).
3.  Observe logs:
    *   `Acquired X slots...`
    *   `Released X slots...`
    *   Ensure NO "Rejected execution" or thread starvation.

