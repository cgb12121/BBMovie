# 10. Add api gateway and eureka server for services discovery

**Status:** Proposed  
**Date:** 2025-07-19 (Decision window: 4 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 0c34e909 (4 commits in window)

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices: 1. **media-service** - Metadata management and source of truth 2. **media-upload-service** - Upload presign URLs and file operations 3. **transcode-worker** - CPU-intensive transcoding tasks to optimize security, cost, and development velocity. The decision was made due to the necessity for better maintainability, scalability, and performance in handling file operations.

**Commit Message:** add api gateway and eureka server for services discovery




**Decision Window:** This ADR represents 4 related commits over 4 days:
- 78aad788: remove unsupported nimbus algo
- d54c4d25: fix upload service v1,v2 for incorrect file name output
- 880e404e: add api gateway and eureka server for services discovery
- 0c34e909: add api gateway and eureka server for services discovery

## Decision

The `file-service` has been retired and replaced with the `media-upload-service`, which handles file uploads by generating presign URLs. This migration helps improve security compliance as it separates the concerns of metadata management from file storage and upload operations. The `transcode-worker` service was introduced to handle CPU-intensive transcoding tasks, further enhancing development velocity.

## Consequences

- Positive: Improved security compliance and maintainability by separating file storage and upload operations
- Positive: Enhanced scalability due to the microservices architecture
- Negative: Trade-off between maintaining a consistent data model across services and the need for separate metadata management in `media-service`
- Negative: Risk of increased complexity if not properly managed during service discovery and communication
## Alternatives Considered

- Retaining the monolithic 'file-service' to maintain consistency with existing codebase
- Using a centralized file service to manage data flow between services, which might lead to a higher risk of security breaches and performance bottlenecks

## Technical Details

- **Commit Hash:** `0c34e909743bee0b93a5f479a9665b545cb2a9eb`
- **Files Changed:** 10
- **Lines Added:** 3022
- **Lines Removed:** 0
- **Affected Areas:** refactor, infrastructure, technology, service

## Related Files

- `backend/authentication/.gitattributes` (+0/-0)
- `backend/authentication/.gitignore` (+0/-0)
- `.../.mvn/wrapper/maven-wrapper.properties` (+0/-0)
- `backend/authentication/mvnw` (+0/-0)
- `backend/authentication/mvnw.cmd` (+0/-0)
- `backend/bbmovie-stream/pom.xml` (+0/-0)
- `.../controller/VideoStreamController.java` (+0/-0)
- `.../com/example/bbmoviestream/entity/Movie.java` (+0/-0)
- `.../bbmoviestream/entity/VideoMetadata.java` (+0/-0)
- `.../bbmoviestream/repository/MovieRepository.java` (+0/-0)

