# 19. - secure cloudinary cloud

**Status:** Proposed  
**Date:** 2025-08-09  
**Deciders:** Cao Gia Bao  
**Commit:** a7dffe21

## Context

The decision to decompose the monolithic file-service into three specialized microservices: media-service, media-upload-service, and media-streaming-service was driven by the need for improved development velocity, code maintainability, and security compliance requirements. The microservice architecture allows for increased scalability, easier maintenance, and better performance.

**Commit Message:** - secure cloudinary cloud





## Decision

The BBMovie platform decided to decompose the legacy 'file-service' into three specialized microservices: media-service, media-upload-service, and media-streaming-service. This decision was made to enhance development velocity, improve code maintainability, and address security compliance requirements. The new architecture enables better scalability, easier maintenance, and improved performance.

## Consequences

- Positive: Improved development velocity
- Positive: Enhanced code maintainability
- Negative: Trade-off between microservice complexity and learning curve
- Negative: Potential risks of increased system complexity
## Alternatives Considered

- Alternative 1: Keeping the 'file-service' monolithic to simplify deployment, testing, and maintenance.
- Alternative 2: Gradually decomposing the 'file-service' into smaller services over time.

## Technical Details

- **Commit Hash:** `a7dffe21daec3664c8efb6abbd47ee85ac098f5d`
- **Files Changed:** 5
- **Lines Added:** 634
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `.../com/bbmovie/auth/controller/MfaController.java` (+0/-0)
- `.../bbmovie/auth/service/auth/mfa/MfaService.java` (+0/-0)
- `.../constraints/ResolutionConstraints.java` (+0/-0)
- `.../controller/FileStreamingController.java` (+0/-0)
- `.../controller/FileUploadController.java` (+0/-0)

