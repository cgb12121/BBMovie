# ADR-002: STS-Style Temporary Credentials for Direct Upload

## Status
Accepted

## Context
The chunked upload approach (ADR-001) requires significant frontend logic to:
- Manage chunk batching
- Handle retries
- Track progress
- Coordinate multipart upload completion

For very large files (GB to TB), this frontend complexity becomes a burden. Additionally, the backend still needs to generate and manage presigned URLs for each batch.

AWS S3 and similar services provide **Security Token Service (STS)** that issues temporary credentials with restricted policies. This allows clients to use SDKs directly, which handle chunking, retry, and multipart upload automatically.

## Decision
We decided to implement an **STS-style temporary credentials** approach:

1. **Single Credential Request**: Client requests temporary credentials once
2. **Policy-Based Restrictions**: Credentials include IAM-style policy limiting upload to specific bucket/object
3. **Direct SDK Usage**: Frontend uses MinIO/AWS SDK with credentials
4. **Automatic Handling**: SDK handles chunking, multipart upload, retry automatically

## Architecture

### API Endpoint
- `POST /upload/sts/credentials` - Get temporary credentials
  - Request: `{ purpose, filename, sizeBytes, contentType, durationSeconds }`
  - Response: `{ uploadId, bucket, objectKey, endpoint, accessKey, secretKey, sessionToken, expiration, region }`

### Policy Structure
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": ["arn:aws:s3:::bucket/object-key"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:CreateMultipartUpload",
        "s3:UploadPart",
        "s3:CompleteMultipartUpload",
        "s3:AbortMultipartUpload"
      ],
      "Resource": ["arn:aws:s3:::bucket/object-key"]
    }
  ]
}
```

### Flow
```
1. Client: POST /upload/sts/credentials
   → Backend: Creates MediaFile record, generates policy
   → Response: { accessKey, secretKey, sessionToken (policy), endpoint, ... }

2. Client: Initialize MinIO SDK with credentials
   → const client = new Minio.Client({ accessKey, secretKey, ... })

3. Client: Upload file using SDK
   → client.putObject(bucket, objectKey, file)
   → SDK automatically:
     - Chunks large files
     - Uploads chunks in parallel
     - Retries failed chunks
     - Completes multipart upload

4. Backend: MediaFile status updated when upload completes
```

## Implementation

### Current Implementation
- **StsCredentialsService**: Generates credentials and policy
- Uses admin credentials with policy restriction (simplified approach)
- Policy stored in `sessionToken` field
- Feature toggles:
  - `upload.enable-sts` (default: true) — disable STS flow when needed.
  - `upload.enable-batch-presign` (default: true) — disable batch presign multipart generation (single-part presign remains).
- `upload.sts.use-admin-service-account` (default: false) — when true, use MinIO Admin API to mint a temporary service account with a tight policy (PutObject/AbortMultipartUpload scoped to bucket/objectKey). Requires admin endpoint/credentials.
- `upload.sts.service-account.expiry-seconds` — expiry for the temporary service account.
- Access control: STS endpoint only allows callers with `ADMIN` role.

### MinIO Limitations
MinIO doesn't have native STS service like AWS. Current implementation:
- Returns admin credentials with policy JSON
- Policy is informational (frontend should validate)
- In production, should use MinIO Admin API to create temporary users

### Future Improvements
1. **MinIO Admin API Integration**: Create temporary users with restricted policies (PutObject/AbortMultipartUpload scoped to user/prefix), revoke on complete/expiry. (Now available behind `upload.sts.use-admin-service-account`.)
2. **Proper STS Service**: Implement STS-compatible service
3. **Credential Rotation**: Support credential refresh before expiration

## Consequences

### Positive
- ✅ **Minimal Backend Load**: Single credential generation
- ✅ **Simple Frontend**: SDK handles all complexity
- ✅ **Better UX**: Automatic progress, retry, chunking
- ✅ **Scalability**: Handles files of any size efficiently
- ✅ **Industry Standard**: Follows AWS S3 pattern

### Negative
- ❌ **MinIO Limitations**: No native STS, requires workaround
- ❌ **Security Considerations**: Currently uses admin credentials (needs improvement)
- ❌ **Less Control**: Backend has less visibility into upload progress
- ❌ **Policy Enforcement**: Policy is informational, not enforced by MinIO

### Mitigations
- Policy JSON provides clear restrictions (client-side validation)
- Credentials expire after configured duration
- MediaFile tracking still provides audit trail
- Future: Implement proper temporary user creation

## Comparison with Chunked Upload

| Aspect | Chunked Upload (ADR-001) | STS Credentials (ADR-002) |
|--------|-------------------------|---------------------------|
| Backend Load | Medium (batch URL generation) | Low (single credential) |
| Frontend Complexity | High (chunking, retry logic) | Low (SDK handles it) |
| Progress Tracking | Detailed (per chunk) | Basic (SDK internal) |
| Retry Control | Fine-grained (per chunk) | Automatic (SDK) |
| Scalability | Good (1000+ chunks) | Excellent (any size) |
| Use Case | Medium files, need control | Large files, simplicity |

## Recommendations

### When to Use Each Approach

**Use Chunked Upload (ADR-001)** when:
- Need detailed progress tracking
- Want fine-grained retry control
- Files are medium-sized (hundreds of MB to few GB)
- Need backend visibility into upload process

**Use STS Credentials (ADR-002)** when:
- Files are very large (GB to TB)
- Simplicity is priority
- Frontend wants minimal code
- Can accept less detailed progress

### Hybrid Approach
Consider allowing clients to choose:
- Small files (< 10MB): Single presigned URL
- Medium files (10MB - 1GB): Chunked upload
- Large files (> 1GB): STS credentials

## References
- Implementation: `StsCredentialsService.java`
- Test: `test/upload-test/test-sts-upload.html`
- Related: ADR-001 (Batch Chunked Upload)

