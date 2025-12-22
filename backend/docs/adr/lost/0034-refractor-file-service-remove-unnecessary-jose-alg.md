# 34. - refractor(file-service) + remove unnecessary jose algorithms + add auto init s

**Status:** Proposed  
**Date:** 2025-10-18 (Decision window: 0 days)  
**Deciders:** Cao Gia Bao  
**Commit:** c8fc4c01 (2 commits in window)

## Context

The decision to decompose the monolithic file-service into media-upload-service, media-service, and media-streaming-service was driven by security compliance requirements. The legacy 'god service' made it challenging to maintain a secure and compliant architecture. Additionally, the cost of running such an inefficient system started posing pressure on the project's budget. To address these challenges, the BBMovie platform opted for microservices architecture, which allows for better scalability, security, and resource efficiency.

**Commit Message:** - refractor(file-service) + remove unnecessary jose algorithms + add auto init sql scheme + implement resilient nats js




**Decision Window:** This ADR represents 2 related commits over 0 days:
- c8fc4c01: - refractor(file-service) + remove unnecessary jose algorith
- 3f76cf05: - refractor(gateway) + remove unsupported jwe algo

## Decision

The file-service was decomposed into three specialized microservices: media-upload-service, media-service, and media-streaming-service to improve the system's maintainability, security, and cost-efficiency. The decision was made to create separate services for each responsibility, which allows us to leverage the benefits of microservices architecture.

## Consequences

- Positive: Improved scalability and resource efficiency.
- Positive: Enhanced security due to better access control.
- Negative: Trade-off between development time and maintenance costs.
- Negative: Risk of introducing new dependencies and increasing deployment complexity.
## Alternatives Considered

- Alternative 1: Retaining the monolithic file-service architecture. This would have resulted in a less secure, scalable, and maintainable system. It also posed challenges with compliance requirements and cost optimization.
- Alternative 2: Decomposing into multiple microservices but not addressing security concerns. Although this approach might have reduced development time, it could lead to potential security issues, making the overall system less reliable.

## Technical Details

- **Commit Hash:** `c8fc4c014fb49c142730ab43dc54fcaf1b174841`
- **Files Changed:** 10
- **Lines Added:** 743
- **Lines Removed:** 0
- **Affected Areas:** service, messaging, refactor, technology, infrastructure

## Related Files

- `backend/file-service/pom.xml` (+0/-0)
- `.../com/bbmovie/fileservice/config/NatsConfig.java` (+0/-0)
- `.../bbmovie/fileservice/config/R2dbcConfig.java` (+0/-0)
- `.../fileservice/dto/event/NatsConnectionEvent.java` (+0/-0)
- `.../repository/FileAssetRepository.java` (+0/-0)
- `.../{FilterOrderConfig.java => FilterOrder.java}` (+0/-0)
- `.../bbmovie/gateway/config/RateLimiterConfig.java` (+0/-0)
- `.../{RedisConfig.java => ReactiveRedisConfig.java}` (+0/-0)
- `.../bbmovie/gateway/config/WebClientConfig.java` (+0/-0)
- `.../gateway/logging/RequestLoggingConfig.java` (+0/-0)

