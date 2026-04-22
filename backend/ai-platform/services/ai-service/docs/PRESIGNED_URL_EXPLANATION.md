# How Presigned URLs Work with MinIO

## Understanding Presigned URLs

Presigned URLs are **self-contained authentication tokens** that allow temporary access to private objects in MinIO/S3 buckets without requiring direct credentials.

## How They Work

### 1. **URL Generation (in `media-upload-service`)**

When `media-upload-service` generates a presigned URL:

```java
minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(mediaFile.getBucket())
        .object(mediaFile.getObjectKey())
        .expiry(1, TimeUnit.HOURS)
        .build()
);
```

**What happens internally:**
- MinIO client uses **service credentials** (`MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`) to sign the URL
- Creates a cryptographic signature using HMAC-SHA256
- Embeds the signature in the URL query parameters:
  - `X-Amz-Algorithm=AWS4-HMAC-SHA256`
  - `X-Amz-Credential=<access-key>/<date>/<region>/s3/aws4_request`
  - `X-Amz-Signature=<signature>`
  - `X-Amz-Expires=3600` (1 hour)

### 2. **Example Presigned URL**

```
https://minio.example.com/bbmovie-ai-assets/ai/assets/uuid123.pdf?
  X-Amz-Algorithm=AWS4-HMAC-SHA256&
  X-Amz-Credential=minioadmin%2F20240115%2Fus-east-1%2Fs3%2Faws4_request&
  X-Amz-Date=20240115T120000Z&
  X-Amz-Expires=3600&
  X-Amz-SignedHeaders=host&
  X-Amz-Signature=<cryptographic-signature>
```

### 3. **Downloading with Presigned URL (in `rust-ai-context-refinery`)**

**✅ NO MinIO credentials needed!** The rust service just needs to:

```rust
let client = reqwest::Client::builder()
    .timeout(Duration::from_secs(300))
    .redirect(Policy::limited(5))
    .build()?;

let resp = client.get(presigned_url).send().await?;
// Download file...
```

**Why this works:**
- The presigned URL **IS the authentication** - signature is in the query string
- MinIO validates the signature when the request arrives
- No need for `Authorization` header or credentials
- Works even if bucket is private (signature proves permission)

### 4. **Security Model**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User uploads file → Gets presigned upload URL            │
│ 2. User uploads directly to MinIO using presigned URL       │
│ 3. User submits chat with uploadId                          │
│ 4. AI service validates user owns file (JWT check)          │
│ 5. AI service gets presigned download URL (service auth)    │
│ 6. Rust service downloads using presigned URL (no auth)     │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Presigned URLs work with **simple HTTP GET** - no special SDK needed
- ✅ Bucket can be private - presigned URL bypasses bucket policy
- ✅ Time-limited (1 hour) - expires automatically
- ✅ User-scoped access - only file owner can get download URL
- ✅ No credentials needed in rust service - URL contains signature

## Why Rust Service Doesn't Need MinIO Credentials

1. **Presigned URL = Authentication Token**
   - The URL itself contains all authentication info
   - MinIO validates the signature embedded in the URL
   - No need for separate credentials

2. **Service-to-Service Flow**
   ```
   User (JWT) → AI Service → media-upload-service → Presigned URL
                                                          ↓
   Rust Service ← HTTP GET (presigned URL) ←─────────────┘
   ```

3. **Bucket Privacy Doesn't Matter**
   - Even if `bbmovie-ai-assets` bucket is private
   - Presigned URL grants temporary access
   - Signature proves the requester has permission (via service credentials)

## Current Implementation Status

### ✅ What's Already Working

1. **`media-upload-service`** generates presigned URLs using service credentials
2. **`ai-service`** fetches presigned URLs with user JWT validation
3. **`rust-ai-context-refinery`** downloads using `reqwest` HTTP client
   - ✅ Handles redirects
   - ✅ Streams large files
   - ✅ Proper error handling

### ⚠️ Potential Issues & Solutions

**Issue 1: Bucket doesn't exist yet**
- **Solution:** Already added to `docker-compose.yml` ✅

**Issue 2: AI_ASSET purpose not defined**
- **Solution:** Need to add `AI_ASSET` to `UploadPurpose` enum
- **Solution:** Need to add bucket configuration for AI assets

**Issue 3: URL expiration**
- **Current:** 1 hour expiry
- **Solution:** Should be sufficient for processing
- **Note:** If processing takes >1 hour, need to regenerate URL

## Testing Presigned URLs

You can test presigned URLs manually:

```bash
# 1. Get presigned URL from media-upload-service
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:6969/upload/files/<uploadId>/url

# 2. Download using presigned URL (no auth needed!)
curl "<presigned-url>" -o downloaded-file.pdf

# 3. Should work even if bucket is private!
```

## Conclusion

**✅ Presigned URLs work perfectly with simple HTTP GET requests**

The rust service **does NOT need MinIO credentials** because:
- Presigned URLs contain authentication in the URL itself
- MinIO validates the signature, not bucket policies
- The signature proves permission via service credentials

The current implementation should work correctly! 🎉

