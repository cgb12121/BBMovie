# 37. - bug(gateway) + wrong credentials on login now return 401 instead of 404

**Status:** Proposed  
**Date:** 2025-10-19  
**Deciders:** Cao Gia Bao  
**Commit:** 10d96c91

## Context

The decision to migrate file operations from the decomposed legacy 'file-service' into the specialized microservices: media-upload-service, media-streaming-service, and payment-service was needed due to security compliance requirements.

**Commit Message:** - bug(gateway) + wrong credentials on login now return 401 instead of 404





## Decision

The file operations were migrated from the monolithic 'file-service' to separate microservices: media-upload-service for presigned URLs, media-streaming-service for streaming files, and payment-service for handling transactions. This migration enhances security by centralizing access control (authentication) and reducing complexity by separating concerns.

## Consequences

- Positive: Improved security through centralized authentication
- Positive: Reduced system complexity by separating file operations into specialized services
## Alternatives Considered

- Alternative 1: Keeping the 'file-service' monolithic to simplify development
- Alternative 2: Decomposing the 'file-service' but leaving authentication control outside of it, which would increase security risk

## Technical Details

- **Commit Hash:** `10d96c919894564cb89bf8e2d4a3e4d28a8a7da2`
- **Files Changed:** 3
- **Lines Added:** 16
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `.../com/bbmovie/auth/controller/advice/GlobalExceptionHandler.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/exception/BadLoginException.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/service/auth/AuthServiceImpl.java` (+0/-0)

