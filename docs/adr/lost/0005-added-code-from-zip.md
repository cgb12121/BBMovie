# 5. Added code from ZIP

**Status:** Proposed  
**Date:** 2025-06-26 (Decision window: 28 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 2e888bb0 (13 commits in window)

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices to improve development velocity, security, reliability, and scalability. The pressure signals driving this decision include VELOCITY for faster code development, SECURITY for compliance requirements, RELIABILITY for fault tolerance needs, and SCALE for performance improvements.

**Commit Message:** Added code from ZIP




**Decision Window:** This ADR represents 13 related commits over 28 days:
- 9f4ad1d0: Update runtime switching strategy for jose token
- 2e283c43: add support for JWK
- 6f8e98e9: init service for handle upload file
- d204da6c: new microservice for handling file upload
- 2e888bb0: Added code from ZIP
... and 8 more commits

## Decision

The file-service has been decomposed by replacing its monolithic architecture with three specialized microservices: media-upload-service to handle presigned URLs, media-streaming-service for streaming media files, and media-service as the source of metadata management. This decision was made to improve system maintainability and scalability while addressing security and compliance requirements.

## Consequences

- Positive: Improved code maintainability and faster development velocity
- Positive: Enhanced security and compliance with separate services handling file operations
- Negative: Trade-off between maintaining legacy codebase and decomposing it into multiple microservices
- Negative: Potential risk of increased complexity and longer deployment cycles due to the need for integration between the new services
## Alternatives Considered

- Alternative 1: Keeping the monolithic 'file-service' architecture, which would have resulted in code duplication, security vulnerabilities, and reduced maintainability.
- Alternative 2: Decomposing the file-service into a single microservice, which would have led to increased complexity and potential performance issues due to inefficient data flow between services.

## Technical Details

- **Commit Hash:** `2e888bb0bc32c25bf6003722d42cbbd3b475fa60`
- **Files Changed:** 10
- **Lines Added:** 17595
- **Lines Removed:** 0
- **Affected Areas:** architecture, technology, infrastructure, service, component, database, refactor

## Related Files

- `backend/pom.xml` (+0/-0)
- `.../com/example/bbmovie/constant/MimeType.java` (+0/-0)
- `.../example/bbmovie/controller/AuthController.java` (+0/-0)
- `.../bbmovie/controller/CloudinaryController.java` (+0/-0)
- `.../controller/DeviceSessionController.java` (+0/-0)
- `.../audit/CachedBodyHttpServletRequest.java` (+0/-0)
- `.../bbmovie/audit/RequestLoggingFilter.java` (+0/-0)
- `.../example/bbmovie/controller/JwksController.java` (+0/-0)
- `.../bbmovie/controller/ProfileController.java` (+0/-0)
- `.../CachedBodyHttpServletRequest.java` (+0/-0)

