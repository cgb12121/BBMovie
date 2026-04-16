# 7. Add support to modify video

**Status:** Proposed  
**Date:** 2025-07-13 (Decision window: 1 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 2a347041 (2 commits in window)

## Context

The decision to decompose the monolithic file-service into specialized microservices was driven by development velocity and maintainability concerns. The project adopted a microservices architecture style to enhance scalability and flexibility. File operations are now handled through media-upload-service, ensuring a cleaner codebase.

**Commit Message:** add support to modify video




**Decision Window:** This ADR represents 2 related commits over 1 days:
- 6794e0d7: fix upload file automatically add prefix file:///
- 2a347041: add support to modify video

## Decision

The BBMovie platform decided to decompose the legacy 'file-service' (monolithic god service) into three specialized microservices: media-upload-service, media-service, and media-streaming-service. These services aim to improve maintainability and scalability by handling specific responsibilities in a modular architecture.

## Consequences

- Positive: The new architecture allows for easier maintenance, faster development velocity, and better separation of concerns.
- Positive: It enables the team to work on multiple features simultaneously without interfering with each other's codebase.
- Negative: There is a trade-off between decomposing services and maintaining compatibility with the existing system. Some changes may be required in order to ensure smooth transition.
- Negative: The migration process might introduce risks, such as data inconsistency or performance degradation during the transition.
## Alternatives Considered

- Alternative 1: Refactoring the monolithic file-service without decomposing it into microservices. However, this would not address the underlying issues of maintainability and scalability.
- Alternative 2: Keeping the monolithic structure and adding additional layers to handle specific responsibilities. This might complicate the codebase and hinder future development.

## Technical Details

- **Commit Hash:** `2a347041bf3aba7578ee6f044e8a0cc4b8cf8d1f`
- **Files Changed:** 8
- **Lines Added:** 1771
- **Lines Removed:** 0
- **Affected Areas:** technology

## Related Files

- `.../example/common/dtos/kafka/FileUploadEvent.java` (+0/-0)
- `.../example/common/dtos/kafka/UploadMetadata.java` (+0/-0)
- `.../constraints/ResolutionConstraints.java` (+0/-0)
- `.../controller/FileUploadController.java` (+0/-0)
- `.../controller/advice/GlobalExceptionHandler.java` (+0/-0)
- `backend/bbmovie-upload-file/pom.xml` (+0/-0)
- `.../bbmovieuploadfile/config/ClamAVConfig.java` (+0/-0)
- `.../bbmovieuploadfile/config/TikaConfig.java` (+0/-0)

