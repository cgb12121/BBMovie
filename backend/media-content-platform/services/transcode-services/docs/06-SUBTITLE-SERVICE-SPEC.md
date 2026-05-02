# Subtitle Processing Service Specification

The `subtitle-service` handles the lifecycle of subtitle assets, ensuring they are normalized, translated, and correctly integrated into the HLS streaming experience.

## 1. Responsibilities

Instead of separate microservices, the `subtitle-service` consolidates multiple **Temporal Activities** into a single deployable unit.

### 1.1 Normalization & Validation (`normalizeSubtitle`)
*   **Input:** Mezzanine subtitle file (SRT, TTML, etc.).
*   **Logic:**
    1. Parses the source file using format-specific parsers.
    2. Converts all entries into a **Pivot JSON Format**.
    3. **Validation:** Checks for overlapping timestamps, minimum/maximum cue duration, and illegal characters.
*   **Storage:** Saves the normalized JSON to `bbmovie-raw/subtitles/{id}/en.json`.

### 1.2 Automated Translation (`translateSubtitle`)
*   **Input:** Pivot JSON (Source) + Target Language.
*   **Logic:**
    1. Sends cue text to a Translation API (LLM/Gemini).
    2. Maintains timestamp integrity while replacing text.
    3. Handles rate-limiting and chunking for long movies.
*   **Storage:** Saves the translated JSON to `bbmovie-raw/subtitles/{id}/{lang}.json`.

### 1.3 Manifest Integration (`integrateSubtitles`)
*   **Role:** This activity is the "Bridge" between subtitle assets and the HLS manifest.
*   **Logic:**
    1. Converts Pivot JSON to **WebVTT** format (required for browser players).
    2. Uploads WebVTT files to `bbmovie-hls/subtitles/{id}/{lang}/file.vtt`.
    3. Updates the `master.m3u8` playlist with `#EXT-X-MEDIA:TYPE=SUBTITLES` entries.
    4. Ensures the `AUTOSELECT=YES` and `DEFAULT` flags are set correctly for the primary language.

## 2. Why "Packaging" is Manifest Integration

Adding subtitles does not require re-transcoding the video bitstreams. Instead, we "package" them as WebVTT and update the pointers in the manifest. This allows us to add new languages (e.g., Vietnamese, Japanese) to a movie years after it was first encoded without touching the large video files.

## 3. Pivot JSON Data Schema

```json
{
  "uploadId": "uuid-v7",
  "language": "en",
  "cues": [
    {
      "start": 1.500,
      "end": 4.200,
      "text": "Hello, world!"
    }
  ]
}
```

## 4. Implementation Details

*   **Language:** Java / Spring Boot.
*   **Queue:** `subtitle-queue`.
*   **Scaling:** Scale horizontally based on the number of languages being translated simultaneously.
