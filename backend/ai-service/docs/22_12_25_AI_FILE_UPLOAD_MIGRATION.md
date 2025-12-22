# AI Service File Upload Migration - Implementation Summary

## Overview

This document describes the migration of AI service file upload flow from `file-service` to `media-upload-service`. The migration allows clients to upload files directly to MinIO using presigned URLs, improving scalability and reducing backend load.

## Changes Made

### 1. New Components Created

#### MediaUploadServiceClientConfig
- **File:** `config/MediaUploadServiceClientConfig.java`
- **Purpose:** Configures WebClient for calling media-upload-service
- **Configuration:** `media-upload-service.url` (default: `lb://media-upload-service`)

#### MediaUploadServiceClient Interface
- **File:** `service/MediaUploadServiceClient.java`
- **Purpose:** Interface for getting file download URLs from media-upload-service
- **Method:** `getDownloadUrl(String uploadId, String jwtToken)`

#### MediaUploadServiceClientImpl
- **File:** `service/impl/MediaUploadServiceClientImpl.java`
- **Purpose:** Implementation that calls `/upload/files/{uploadId}/url` endpoint
- **Returns:** Presigned download URL for the file

### 2. Updated Components

#### ChatRequestDto.FileAttachment
- **File:** `dto/request/ChatRequestDto.java`
- **Changes:**
  - Added `uploadId` (String) - New primary field for file identification
  - `fileUrl` (String) - Optional, can be provided directly or fetched using uploadId
  - `fileId` (Long) - Deprecated, kept for backward compatibility

#### FileProcessingService Interface
- **File:** `service/FileProcessingService.java`
- **Changes:**
  - Updated method signature to include `Jwt jwt` parameter
  - Method: `processAttachments(List<FileAttachment> attachments, Jwt jwt)`

#### FileProcessingServiceImpl
- **File:** `service/impl/rust/worker/FileProcessingServiceImpl.java`
- **Changes:**
  - Removed `FileUploadClient` dependency (no longer calls file-service)
  - Added `MediaUploadServiceClient` dependency
  - Removed file confirmation step (files confirmed on upload)
  - Added logic to fetch download URLs from media-upload-service when `uploadId` is provided
  - Supports backward compatibility with legacy `fileId` field

#### ChatServiceImpl
- **File:** `service/impl/ChatServiceImpl.java`
- **Changes:**
  - Updated to pass JWT token to `FileProcessingService.processAttachments()`

### 3. Configuration Updates

#### application.properties
- **File:** `src/main/resources/application.properties`
- **Added:**
  ```properties
  media-upload-service.url=http://localhost:6969
  ```

## New Flow

### Client-Side Flow

1. **Get Presigned URL**
   ```typescript
   POST /upload/init
   {
     "purpose": "AI_ASSET",
     "filename": "document.pdf",
     "contentType": "application/pdf",
     "sizeBytes": 1048576,
     "checksum": "sha256:...",
     "sparseChecksum": "sha256:..."
   }
   
   Response:
   {
     "uploadId": "018c64d8-7b9e-7123-8456-123456789abc",
     "objectKey": "ai/assets/user-123/018c64d8-7b9e-7123-8456-123456789abc.pdf",
     "uploadUrl": "https://minio.../bbmovie-ai-assets/...?X-Amz-Algorithm=...",
     "expiresAt": "2024-01-15T10:30:00Z"
   }
   ```

2. **Upload to MinIO**
   ```typescript
   PUT {uploadUrl}
   Content-Type: application/pdf
   [binary file data]
   ```

3. **Submit Chat with File Reference**
   ```typescript
   POST /api/v1/chat/{sessionId}
   {
     "message": "Analyze this document",
     "attachments": [
       {
         "uploadId": "018c64d8-7b9e-7123-8456-123456789abc",
         "filename": "document.pdf"
       }
     ]
   }
   ```

### Backend Flow

1. **ChatController** receives request with `FileAttachment` containing `uploadId`
2. **ChatServiceImpl** calls `FileProcessingService.processAttachments(attachments, jwt)`
3. **FileProcessingServiceImpl**:
   - For each attachment:
     - If `fileUrl` is provided → use it directly
     - If `uploadId` is provided → call `MediaUploadServiceClient.getDownloadUrl(uploadId, jwtToken)`
     - If only legacy `fileId` → convert to string and fetch URL (backward compatibility)
   - Prepares `RustProcessRequest` list with file URLs
   - Calls `RustAiContextRefineryClient.processBatch(requests)`
4. **Rust Service** downloads files from URLs and processes them
5. **FileProcessingServiceImpl** formats results and returns to ChatService

## Backward Compatibility

The implementation maintains backward compatibility:

1. **Legacy `fileId` Support:**
   - If `uploadId` is null but `fileId` exists, converts `fileId` to string
   - Logs warning about deprecated field
   - Still attempts to fetch URL (may fail if file-service is removed)

2. **Direct `fileUrl` Support:**
   - If `fileUrl` is provided, uses it directly
   - No need to fetch from media-upload-service
   - Useful for testing or special cases

## Migration Checklist

- [x] Create MediaUploadServiceClientConfig
- [x] Create MediaUploadServiceClient interface
- [x] Create MediaUploadServiceClientImpl
- [x] Update FileAttachment DTO
- [x] Update FileProcessingService interface
- [x] Update FileProcessingServiceImpl
- [x] Update ChatServiceImpl
- [x] Add configuration property
- [x] Update test HTML to use new upload flow (`test-upload.html`, `test-chat.html`)
- [x] Remove FileUploadClient (legacy file-service client)
- [x] Remove FileServiceClientConfig (legacy file-service WebClient)

## Testing

### Unit Tests Needed

1. `MediaUploadServiceClientImplTest`
   - Test successful URL retrieval
   - Test error handling
   - Test authentication

2. `FileProcessingServiceImplTest`
   - Test with uploadId (fetch URL)
   - Test with direct fileUrl
   - Test with legacy fileId (backward compatibility)
   - Test error handling

### Integration Tests Needed

1. End-to-end chat flow with file attachments
2. Verify rust service receives correct URLs
3. Verify file processing completes successfully

## Rollback Plan

If issues occur:

1. **Quick Rollback:** Revert to using `fileId` (Long) in FileAttachment
2. **Keep Old Code:** FileUploadClient still exists, can be re-enabled
3. **Feature Flag:** Add property to toggle between old/new flow

## Next Steps

1. **Frontend Migration:**
   - Update upload flow to use presigned URLs
   - Update chat request to use `uploadId` instead of `fileId`

2. **Cleanup:**
   - Remove FileUploadClient after frontend migration
   - Remove FileServiceClientConfig
   - Remove file-service.url configuration

3. **Monitoring:**
   - Monitor error rates for URL fetching
   - Monitor rust service download success rates
   - Track migration progress

## Rust Service Updates

### Changes Made to rust-ai-context-refinery

**File:** `backend/rust-ai-context-refinery/src/utils.rs`

**Improvements:**
1. **Enhanced HTTP Client Configuration:**
   - Added timeout (5 minutes) for large file downloads
   - Added redirect following (up to 5 redirects) for MinIO compatibility
   - Better error handling for HTTP errors

2. **Streaming Download:**
   - Changed from loading entire file into memory to streaming
   - More memory efficient for large files
   - Uses `bytes_stream()` instead of `bytes().await`

3. **Better Error Messages:**
   - Detailed error messages for debugging
   - Status code checking before processing
   - Error text extraction from failed responses

**Why These Changes:**
- MinIO presigned URLs work with standard HTTP GET requests (authentication in query string)
- However, MinIO might use redirects or have different timeout requirements
- Streaming prevents memory issues with large files (PDFs, videos, etc.)

**Cargo.toml Update:**
- Added `"stream"` feature to `reqwest` dependency for streaming support

## Notes

- Rust service now properly handles MinIO presigned URLs with improved error handling
- JWT token is required to get download URLs (for access control)
- Files are stored in `bbmovie-ai-assets` bucket (private, user-scoped)
- Presigned URLs are time-limited (1 hour default)
- Rust service streams downloads to disk (memory efficient)

