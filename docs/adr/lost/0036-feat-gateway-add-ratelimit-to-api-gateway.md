# 36. - feat(gateway) + add ratelimit to api gateway

**Status:** Proposed  
**Date:** 2025-10-19  
**Deciders:** Cao Gia Bao  
**Commit:** 3463eaf2

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices: 1. **media-service** - Metadata management and source of truth 2. **media-upload-service** - Upload presign URLs and file handling 3. **transcode-worker** - CPU-intensive tasks such as video transcoding to support various devices and resolutions due to security and compliance requirements.

**Commit Message:** - feat(gateway) + add ratelimit to api gateway





## Decision

The BBMovie platform decided to decompose the monolithic 'file-service' into three microservices: media-upload-service for presigned URLs, media-service for metadata management, and transcode-worker for CPU-intensive tasks like video transcoding. This decision was made to enhance security, scalability, and maintainability of the system.

## Consequences

- Positive: Improved security through better access control and compliance with data handling practices.
- Positive: Scalable architecture that can handle increased user load without downtime.
- Negative: Potential trade-off in terms of learning curve and complexity during migration.
- Negative: Risk of introducing new bugs or performance issues due to the decomposed system's increased surface area.
## Alternatives Considered

- Alternative 1: Keeping the monolithic 'file-service' for simplicity and reduced maintenance burden.
- Alternative 2: Decomposing into a single service with transcode functionality, leaving file handling as part of media-upload-service.

## Technical Details

- **Commit Hash:** `3463eaf29b941f59b9df5b21bbea5f09f6dd03ff`
- **Files Changed:** 5
- **Lines Added:** 499
- **Lines Removed:** 0
- **Affected Areas:** service, infrastructure, technology

## Related Files

- `.../controller/advice/GlobalExceptionHandler.java` (+0/-0)
- `.../bbmovie/auth/security/jose/KeyRotation.java` (+0/-0)
- `.../jose/filter/JoseAuthenticationFilter.java` (+0/-0)
- `.../student/StudentVerificationService.java` (+0/-0)
- `backend/auth/src/main/resources/application.yml` (+0/-0)

