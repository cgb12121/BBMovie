# 25. - add feat student status account

**Status:** Proposed  
**Date:** 2025-08-14  
**Deciders:** Cao Gia Bao  
**Commit:** b239b6f3

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices: security and compliance requirements to ensure data integrity and protect user information. The media-upload-service handles file uploads through presigned URLs, while media-streaming-service manages streaming of files.

**Commit Message:** - add feat student status account





## Decision

The BBMovie project decided to decompose the monolithic file-service into the media-upload-service, media-streaming-service, and retire the retired services (file-service). This decision was made to improve security and compliance, enhance modularity, scalability, and maintainability of the system. The media-upload-service handles file uploads through presigned URLs, while media-streaming-service manages streaming of files.

## Consequences

- Positive: Improved security and data protection
- Positive: Enhanced modularity, scalability, and maintainability.
- Negative: Potential trade-off between performance and complexity.
- Negative: Risks associated with decomposing monolithic architecture.
## Alternatives Considered

- Alternative 1: Retaining the monolithic file-service to simplify development and reduce costs.
- Alternative 2: Decomposing into multiple services but retaining a single monolithic backend for easier maintenance.

## Technical Details

- **Commit Hash:** `b239b6f3a023d972c6209b71813dd1d69c9275fa`
- **Files Changed:** 5
- **Lines Added:** 580
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `.../auth/controller/StudentProgramController.java` (+0/-0)
- `.../bbmovie/auth/dto/StudentApplicationObject.java` (+0/-0)
- `.../{service/student => dto}/UniversityObject.java` (+0/-0)
- `.../com/bbmovie/auth/entity/StudentProfile.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/entity/User.java` (+0/-0)

