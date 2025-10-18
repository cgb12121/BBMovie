# Refactoring Plan: Hybrid Transcoding Model

This document outlines a plan to refactor the video processing pipeline from a pure "pre-computation" model to a more efficient and scalable "hybrid" model.

## 1. Current Problem: Pre-computation Inefficiency

The current implementation follows a "transcode-on-upload" or "pre-computation" strategy:

1.  A user uploads a high-resolution video.
2.  The system immediately and synchronously transcodes this video into **all** possible lower resolutions (1080p, 720p, 480p, etc.).
3.  All of these versions are stored permanently.

While this ensures that video playback is always fast (as the files are always ready), it has two major drawbacks:

-   **High Storage Cost:** Storing every possible resolution for every video consumes a massive amount of disk space. A single 10GB upload can easily become 25GB+ of stored data.
-   **Wasted Compute Resources:** Significant CPU time is spent creating video resolutions that may never be watched by any user.

## 2. Proposed Solution: Hybrid Transcoding Model

The proposed solution is a hybrid model that combines the best of pre-computation and on-demand generation. This is the standard approach used by major video platforms.

**The Logic:**

1.  **Store the Original:** The original, highest-quality uploaded video is always stored and serves as the master source file.
2.  **Pre-compute a Standard Set:** Upon upload, a background job transcodes **only a small, strategic set** of the most common resolutions. For example:
    -   A high-quality web version (e.g., `1080p`).
    -   A standard mobile version (e.g., `480p`).
3.  **Generate Others On-Demand with Caching:** When a user requests a resolution that has *not* been pre-computed (e.g., `720p`):
    -   The system first checks if a cached version (`my_movie_720p.mp4`) already exists.
    -   If it exists, it's streamed directly.
    -   If it **does not exist**, an FFmpeg process is launched **at that moment** to transcode the original master file down to the requested `720p` quality.
    -   The newly transcoded `720p` file is then **saved to disk (cached)** and streamed to the user.
    -   The next user who requests `720p` will get the cached version instantly.

**Benefits:**
-   **Balanced Storage:** Dramatically reduces storage costs by not creating unpopular resolution files.
-   **Efficient Compute:** CPU cycles are only spent on creating resolutions that users actually request.
-   **Good User Experience:** Most users will request one of the standard, pre-computed versions and get instant playback. Only the *first* user to request a rare resolution will experience a short initial delay.

## 3. Implementation Plan

This refactoring would primarily impact the `FileStreamingService` and the `FileUploadService`.

### Step 1: Modify the Upload Process (`FileUploadService`)

-   The `orchestrateUpload` method needs to be changed.
-   After the initial file is saved and validated, it should **not** trigger transcoding for all resolutions.
-   Instead, it should either:
    a) Trigger a limited, asynchronous transcoding job for just the standard resolutions (e.g., 1080p and 480p).
    b) Do no transcoding at all, and rely purely on on-demand generation.

### Step 2: Refactor the Streaming Logic (`FileStreamingService`)

This is the most significant change.

-   The `streamFileByMovieId` method will become the core of the new logic.
-   When a request for a movie and a specific quality comes in (e.g., `movieId=123`, `quality=720p`), the service must perform the following steps:

    1.  **Look up the Master File:** Query the `FileAsset` repository to find the path to the original, highest-resolution master file for `movieId=123`.
    2.  **Construct Target Path:** Construct the expected path for the requested quality (e.g., `/var/www/videos/my_movie_720p.mp4`).
    3.  **Check Cache (File System):** Check if the file at the target path already exists.
    4.  **If Exists:** The file is already cached. Proceed with the existing `streamLocalFile` logic to stream it with range support.
    5.  **If Not Exists (On-Demand Transcoding):**
        a. Return an `HTTP 202 Accepted` immediately to the client to let it know processing has started (or hold the connection, which is simpler but can time out).
        b. Launch a new, asynchronous transcoding job (`VideoTranscoderService.transcode`) to convert the master file to the target resolution and save it to the target path.
        c. **(Advanced)** The ideal implementation would stream the output of the FFmpeg process *directly* to the user's response while simultaneously writing it to the cache file. This is complex but provides the fastest time-to-first-byte.
        d. Once the new file is created, the user's client can be notified to retry the request, at which point it will hit the cache and stream normally.

### Step 3: Cache Management

-   A new scheduled task (`OnDemandCacheCleanupService`) should be created.
-   This task will periodically scan the video storage directory and delete on-demand transcoded files that haven't been accessed in a long time (e.g., 30 days) to reclaim storage space. The original master files and the pre-computed standard resolution files should never be deleted by this process.
