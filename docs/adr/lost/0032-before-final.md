# 32. - before final

**Status:** Proposed  
**Date:** 2025-09-28 (Decision window: 7 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 93736e00 (5 commits in window)

## Context

The BBMovie platform experienced increased development velocity and code maintainability after decomposing the monolithic 'file-service' (legacy 'god service') into three specialized microservices: media-service, media-upload-service, and transcode-worker. The decision to migrate file-service was driven by security requirements, compliance, and improved data flow inconsistencies.

**Commit Message:** - before final




**Decision Window:** This ADR represents 5 related commits over 7 days:
- 93736e00: - before final
- 2016aba0: - fix oauth2 login route mis-configured, fix double cors ori
- 93277f08: - fix oauth2 redirect return mismatch message with FE
- 94d8a1e6: - fix crash on start when nats js not connected
- 470bd245: - add lazy nats setup on startup

## Decision

The BBMovie platform migrated the 'file-service' to decompose it into three specialized microservices: **media-upload-service** for upload presign URLs and file management, **transcode-worker** for CPU-intensive tasks like transcoding, and **media-service** as the metadata source of truth. This decision was made to improve security and compliance requirements while maintaining a modular architecture.

## Consequences

- Positive: Improved security and compliance with microservices architecture
- Positive: Easier maintenance and scalability due to separation of concerns
- Negative: Potential trade-off in increased complexity during initial migration phase
- Negative: Risk of data flow inconsistencies due to potential miscommunication between services
## Alternatives Considered

- Alternative 1: Keeping the monolithic 'file-service' which could lead to security and compliance issues, as well as hinder future scalability.
- Alternative 2: Migrating file-service to a single microservice instead of decomposing it into multiple services. This alternative would have simplified the migration process but might not provide the desired separation of concerns.

## Technical Details

- **Commit Hash:** `93736e0007bd204f7ea5b7d7d7113f556e1f7910`
- **Files Changed:** 10
- **Lines Added:** 7286
- **Lines Removed:** 0
- **Affected Areas:** technology, service, infrastructure, component, security, messaging

## Related Files

- `.gitignore` (+0/-0)
- `backend/bbmovie-common/pom.xml` (+0/-0)
- `.../com/example/common/utils/IpAddressUtils.java` (+0/-0)
- `.../BbmovieSearchApplicationTests.java` (+0/-0)
- `backend/file-service/.gitignore` (+0/-0)
- `.../java/com/bbmovie/auth/security/CorsConfig.java` (+0/-0)
- `backend/auth/src/main/resources/application.yml` (+0/-0)
- `.../service/streaming/FileStreamingService.java` (+0/-0)
- `.../com/bbmovie/gateway/config/CorsConfig.java` (+0/-0)
- `backend/gateway/src/main/resources/application.yml` (+0/-0)

