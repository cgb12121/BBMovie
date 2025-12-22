# 39. - refractor(gateway): remove api key and token null check

**Status:** Proposed  
**Date:** 2025-10-20 (Decision window: 3 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 59e3f8a6 (5 commits in window)

## Context

The decision to decompose the monolithic 'file-service' into three specialized microservices: media-service, media-upload-service, and media-streaming-service was driven by SECURITY concerns for compliance requirements and COST optimization to improve resource efficiency.

**Commit Message:** - refractor(gateway): remove api key and token null check




**Decision Window:** This ADR represents 5 related commits over 3 days:
- 59e3f8a6: - refractor(gateway): remove api key and token null check
- e8a34976: - refractor(gateway): remove api key and token null check
- 9087f2dd: - refractor quarkus eureka connector
- cf8c6123: - demo AI service
- bfaac20b: - demo AI service

## Decision

The file-service has been refactored to separate its responsibilities into three different microservices: media-upload-service is responsible for generating presigned URLs for file uploads, media-service manages metadata and serves as the source of truth, and media-streaming-service handles live streaming. This decision was made to improve maintainability, scalability, and security.

## Consequences

- Positive: Improved security by separating concerns and enforcing compliance
- Positive: Increased scalability due to horizontal scaling of individual microservices
- Negative: Trade-off between development time and refactoring effort
- Negative: Risk of introducing new dependencies
## Alternatives Considered

- Refactor the file-service within the same monolithic architecture
- Keep the file-service as it is without separating concerns

## Technical Details

- **Commit Hash:** `59e3f8a6a84f53c90f82e4bc3059c22ccb23f8ed`
- **Files Changed:** 10
- **Lines Added:** 2600
- **Lines Removed:** 0
- **Affected Areas:** service, refactor, infrastructure, technology

## Related Files

- `.../gateway/config/ApplicationFilterOrder.java` (+0/-0)
- `.../com/bbmovie/gateway/config/FilterOrder.java` (+0/-0)
- `...iptionPlanResolver.java => ClaimsResolver.java}` (+0/-0)
- `.../config/ratelimit/RateLimitingFilter.java` (+0/-0)
- `.../gateway/logging/RequestLoggingConfig.java` (+0/-0)
- `.../bbmovie/gateway/config/WebClientConfig.java` (+0/-0)
- `.../ratelimit/RedisConnectionHealthEvent.java` (+0/-0)
- `.../InvalidAuthenticationMethodException.java` (+0/-0)
- `...uthHeaderFilter.java => AuthHeaderMutator.java}` (+0/-0)
- `.../gateway/security/JwtBlacklistFilter.java` (+0/-0)

