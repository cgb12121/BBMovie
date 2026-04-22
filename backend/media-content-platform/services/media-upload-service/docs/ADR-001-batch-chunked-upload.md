# ADR-001: Batch Chunked Upload with Lazy URL Generation

## Status
Accepted

## Context
When uploading large files (several GB or TB), the previous approach of generating all presigned URLs upfront had several issues:

1. **Performance**: Generating hundreds or thousands of presigned URLs at initialization caused significant backend load
2. **Scalability**: For very large files (1000+ chunks), the initial response could be several MB in size
3. **Network Efficiency**: Clients might not need all URLs if upload fails early
4. **Resource Management**: URLs expire after 24 hours, but many might never be used

The original chunked upload implementation returned all chunk URLs in the initialization response, which didn't scale well for very large files.

## Decision
We decided to implement a **lazy URL generation** approach with **batch fetching**:

1. **Initialization**: Backend only creates the multipart upload session and initializes chunk tracking records (no URLs generated)
2. **Batch URL Fetching**: Frontend requests URLs in batches (e.g., 10 chunks at a time) via `GET /upload/{uploadId}/chunks?from=1&to=10`
3. **On-Demand Generation**: URLs are generated only when requested, and cached in the database with expiration
4. **Chunk Tracking**: Each chunk's status (PENDING, UPLOADING, UPLOADED, FAILED, RETRYING) is tracked in the database
5. **Retry Support**: Failed chunks can be retried individually via `POST /upload/{uploadId}/chunks/{partNumber}/retry`

## Architecture

### Database Schema
- **multipart_upload_sessions**: Tracks multipart upload sessions
  - Stores MinIO upload ID, total chunks, chunk size, expiration
- **chunk_upload_status**: Tracks individual chunk status
  - Stores part number, status, ETag, upload URL, retry count, error messages

### API Endpoints
- `POST /upload/chunked/init` - Initialize chunked upload (no URLs returned)
- `GET /upload/{uploadId}/chunks?from={from}&to={to}` - Get batch of chunk URLs
- `POST /upload/{uploadId}/chunks/{partNumber}/complete` - Mark chunk as uploaded with ETag
- `POST /upload/{uploadId}/chunks/{partNumber}/retry` - Retry failed chunk
- `GET /upload/{uploadId}/chunks/status` - Get upload progress
- `POST /upload/chunked/complete` - Complete multipart upload

### Flow
```
1. Client: POST /upload/chunked/init
   → Backend: Creates session, initializes chunk statuses (PENDING)
   → Response: { uploadId, totalChunks, chunkSizeBytes, totalSizeBytes }

2. Client: GET /upload/{uploadId}/chunks?from=1&to=10
   → Backend: Generates URLs for chunks 1-10 (if not exists/expired)
   → Response: { chunks: [{ partNumber, uploadUrl, startByte, endByte }] }

3. Client: Uploads chunks 1-10 to MinIO
   → For each successful upload: POST /upload/{uploadId}/chunks/{partNumber}/complete?etag=...

4. Client: Repeat steps 2-3 for remaining chunks

5. Client: POST /upload/chunked/complete
   → Backend: Gets all uploaded parts from tracking, completes multipart upload
```

## Consequences

### Positive
- ✅ **Reduced Backend Load**: URLs generated on-demand, not all at once
- ✅ **Better Scalability**: Can handle files with thousands of chunks
- ✅ **Network Efficiency**: Smaller initial response, only fetch what's needed
- ✅ **Resilience**: Individual chunk retry without affecting others
- ✅ **Progress Tracking**: Real-time progress monitoring via status endpoint
- ✅ **Resource Management**: URLs cached and reused if still valid

### Negative
- ❌ **Additional API Calls**: Frontend needs to make more requests
- ❌ **Complexity**: More moving parts (tracking, retry logic)
- ❌ **Database Overhead**: Additional tables and records to maintain

### Mitigations
- Batch size (10 chunks) balances API calls vs. response size
- Chunk status tracking enables better error handling and monitoring
- URL caching reduces redundant generation

## Implementation Details

### Service Layer
- **ChunkedUploadService**: Handles all chunk-related operations
  - Lazy URL generation with caching
  - Chunk status tracking
  - Retry logic
  - Progress calculation

### Frontend Integration
- Frontend requests URLs in batches
- Tracks which chunks are uploaded
- Automatically retries failed chunks
- Shows real-time progress

## Alternatives Considered

1. **All URLs Upfront** (Original)
   - ❌ Doesn't scale for very large files
   - ❌ Wastes resources for unused URLs

2. **Single URL per Request**
   - ❌ Too many API calls
   - ❌ Higher latency

3. **Streaming URL Generation**
   - ⚠️ More complex, requires WebSocket or SSE
   - ✅ Could be future improvement

## References
- Issue: `backend/media-upload-service/issues_upload_large_file.txt`
- Implementation: `ChunkedUploadService.java`
- Test: `test/upload-test/test-upload.html`

