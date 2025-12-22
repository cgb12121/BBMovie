# 33. [Major Change] Refactor JOSE implementation to a single provider

**Status:** Proposed  
**Date:** 2025-10-18 (Decision window: 12 days)  
**Deciders:** Cao Gia Bao  
**Commit:** d31d4845 (17 commits in window)

## Context

The decision to decompose the monolithic file-service into three specialized microservices: media-upload-service, media-service, and media-streaming-service was driven by various pressure signals such as velocity, security, cost optimization, reliability, system scalability, and performance requirements. The BBMovie platform's development velocity had slowed down due to the complexity of managing different file operations within the monolithic file-service. By decomposing it into specialized microservices, the team aimed to improve code maintainability and enhance overall system architecture.

**Commit Message:** [Major Change] Refactor JOSE implementation to a single provider

**Details:** This major refactoring simplifies the entire token management system in the `auth` service by removing the complex strategy-switching mechanism and consolidating to a single, robust JOSE provider.


**Decision Window:** This ADR represents 17 related commits over 12 days:
- 3e5b2662: - update kNN search to filter results on age, region, etc...
- a3605d3b: - fix tomcat dependency issue override netty - update simila
- 6f9f7e0a: - add seeder for dev - simplify ES config as ES client reini
- 20951076: - add seeder for dev - simplify ES config as ES client reini
- 4b96b580: - rename group id of search service
... and 12 more commits

## Decision

The file-service was decomposed by removing direct DB access between services, implementing a presigned URL approach for media uploads through the media-upload-service, and adopting an event-driven communication model using NATS JetStream. This allows each microservice to focus on its specific responsibilities while enabling seamless communication between them.

## Consequences

- Positive: Improved development velocity and code maintainability
- Positive: Enhanced security compliance requirements due to better controlled access to files and metadata
- Negative: Trade-off between performance and scalability - NATS JetStream may not be as efficient or scalable as Kafka, but it is easier to manage and has lower operational costs.
- Negative: Potential risk of introducing data consistency issues across different services during the migration process.
## Alternatives Considered

- Alternative 1: Retaining the monolithic file-service architecture. However, this decision would have hindered further development velocity, security, and scalability requirements.
- Alternative 2: Migrating to Kafka instead of NATS JetStream. While Kafka is more scalable and efficient, it has higher operational costs and a steeper learning curve compared to NATS JetStream.

## Technical Details

- **Commit Hash:** `d31d4845d570ec721458f6c2691483fd56fc1838`
- **Files Changed:** 10
- **Lines Added:** 103450
- **Lines Removed:** 0
- **Affected Areas:** technology, infrastructure, service, messaging, refactor, database, architecture, security

## Related Files

- `backend/bbmovie-search/pom.xml` (+0/-0)
- `.../bbmoviesearch/config/ReactiveRedisConfig.java` (+0/-0)
- `.../bbmoviesearch/config/RestTemplateConfig.java` (+0/-0)
- `.../bbmoviesearch/config/VectorStoreConfig.java` (+0/-0)
- `.../bbmoviesearch/config/WebClientConfig.java` (+0/-0)
- `backend/bbmovie-common/pom.xml` (+0/-0)
- `.../example/common/dtos/nats/UploadMetadata.java` (+0/-0)
- `.../config/ai_vector/ElasticsearchConfig.java` (+0/-0)
- `.../example/bbmoviesearch/dto/PageResponse.java` (+0/-0)
- `.../example/bbmoviesearch/dto/SearchCriteria.java` (+0/-0)

