# 24. - fixed batch processing for saving 80k entries to db

**Status:** Proposed  
**Date:** 2025-08-14 (Decision window: 1 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 6a46525e (5 commits in window)

## Context

The decision to decompose the monolithic file-service into three specialized microservices - media-service, media-upload-service, and media-streaming-service - was needed due to BBMovie's growing scale and security requirements. The file-service faced performance bottlenecks and lacked proper separation of concerns, making it difficult to maintain.

**Commit Message:** - fixed batch processing for saving 80k entries to db




**Decision Window:** This ADR represents 5 related commits over 1 days:
- 434d703a: - fixed batch processing for saving 80k entries to db
- 6a46525e: - fixed batch processing for saving 80k entries to db
- d7f70826: - fixed batch processing for saving 80k entries to db
- 89a209c8: - fixed batch processing for saving 80k entries to db
- 9a6aa2f1: - fixed batch processing for saving 80k entries to db

## Decision

The BBMovie platform decomposed the monolithic 'file-service' into three specialized microservices: media-upload-service for handling file uploads, presign URLs, and storage; media-streaming-service for serving media content; and media-service for metadata management. This decision aimed to enhance performance, scalability, security, and maintainability of the BBMovie platform.

## Consequences

- Positive: Improved performance through better resource utilization and reduced latency in file operations.
- Positive: Enhanced scalability as individual services can be scaled independently based on their needs.
- Negative: Trade-off between development time and maintenance complexity for specialized microservices.
- Negative: Risk of data inconsistency due to the complex data flow across services after decomposing the monolithic 'file-service'.
## Alternatives Considered

- Alternative 1: Keeping the file-service as it was. However, this would have resulted in a slower and less scalable platform with poor security.
- Alternative 2: Decompose the file-service into separate microservices but keep some components within the monolithic architecture. This alternative could have led to inconsistent data flow and performance issues.

## Technical Details

- **Commit Hash:** `6a46525ecad8a50217f5b39b73e0bee8b8b9bb5d`
- **Files Changed:** 10
- **Lines Added:** 81933
- **Lines Removed:** 0
- **Affected Areas:** database

## Related Files

- `.../com/bbmovie/auth/service/student/UniversityRegistry.java` (+0/-0)
- `.../student/StudentVerificationService.java` (+0/-0)
- `.../auth/service/student/UniversityRegistry.java` (+0/-0)
- `backend/auth/src/main/resources/data/uni.json` (+0/-0)
- `.../auth/controller/StudentProgramController.java` (+0/-0)
- `.../controller/StudentVerificationController.java` (+0/-0)
- `.../dto/response/CountryUniversityResponse.java` (+0/-0)
- `.../dto/response/UniversityLookupResponse.java` (+0/-0)
- `.../auth/dto/response/UniversitySummary.java` (+0/-0)
- `.../auth/repository/UniversityRepository.java` (+0/-0)

