# 26. - add feat student status account

**Status:** Proposed  
**Date:** 2025-08-14  
**Deciders:** Cao Gia Bao  
**Commit:** ee1fd7b9

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices to address SECURITY and compliance requirements. The file operations must now go through media-upload-service which generates presigned URLs for secure file uploads.

**Commit Message:** - add feat student status account





## Decision

The retired 'file-service' has been replaced by a new architecture consisting of the following services: - **media-service** - Metadata management and source of truth - **media-upload-service** - Upload presign URLs and file upload handling - **media-streaming-service** - Streaming media content to clients

## Consequences

- Positive: Enhanced security for file operations as presigned URLs are used instead of direct file access.
- Positive: Better compliance with data protection laws.
- Negative: Trade-off between performance and complexity. The new microservices architecture may introduce latency in the data flow due to the additional processing steps.
- Negative: Risk of introducing new vulnerabilities if not properly secured.
## Alternatives Considered

- Alternative 1: Keeping the 'file-service' as is, which would have resulted in continued direct file access and potential security issues.
- Alternative 2: Using a single presigned URL for all file operations across the platform, which could introduce security risks if not properly managed.

## Technical Details

- **Commit Hash:** `ee1fd7b935fc4611cb85d79482a753f064aec108`
- **Files Changed:** 4
- **Lines Added:** 27
- **Lines Removed:** 0
- **Affected Areas:** technology

## Related Files

- `.../com/bbmovie/auth/controller/StudentProgramController.java` (+0/-0)
- `.../auth/service/student/StudentVerificationService.java` (+0/-0)
- `backend/bbmovie-search/pom.xml` (+0/-0)
- `.../example/bbmoviesearch/BbmovieSearchApplicationTests.java` (+0/-0)

