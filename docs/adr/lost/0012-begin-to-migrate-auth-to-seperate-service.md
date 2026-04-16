# 12. Begin to migrate auth to seperate service

**Status:** Proposed  
**Date:** 2025-07-21 (Decision window: 6 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 6d8a7e31 (12 commits in window)

## Context

The decision to decompose the monolithic 'file-service' into three specialized microservices: media-service, media-upload-service, and transcode-worker was driven by security and compliance requirements, cost optimization and resource efficiency, as well as development velocity and maintainability of code. The BBMovie platform has become more complex with an increasing number of services and dependencies, making it necessary to refactor the architecture for better scalability and easier management.

**Commit Message:** begin to migrate auth to seperate service




**Decision Window:** This ADR represents 12 related commits over 6 days:
- 82cf6b6e: fix eureka client for quarkus, remove micronaut service due 
- 6d8a7e31: begin to migrate auth to seperate service
- 1645bbf0: migrating auth to separate service
- 1d7c95d2: migrating auth to separate service
- e3a0af72: migrating auth to separate service
... and 7 more commits

## Decision

The file-service was decomposed by creating three separate microservices: media-upload-service, transcode-worker, and media-streaming-service. This decision aimed to improve the platform's security and compliance by separating concerns and implementing proper authentication and authorization mechanisms. Additionally, it optimized costs and resources by leveraging NATS JetStream for event-driven communication instead of Kafka, reducing unnecessary dependency on a single message broker.

## Consequences

- Positive: Improved security and compliance
- Positive: Cost optimization and resource efficiency
- Negative: Development velocity might be affected due to the need to refactor existing services and update dependencies.
- Negative: Risk of introducing new bugs or issues during migration
## Alternatives Considered

- Alternative 1: Keep the 'file-service' monolithic, which would have resulted in a less secure platform with higher costs and slower development velocity.
- Alternative 2: Migrate to a different message broker like Apache Pulsar or RabbitMQ instead of NATS JetStream. However, this alternative was not chosen due to its increased complexity and potential for performance issues.

## Technical Details

- **Commit Hash:** `6d8a7e31d2d78f0de6041fef6ef47a2459198ba5`
- **Files Changed:** 10
- **Lines Added:** 13437
- **Lines Removed:** 0
- **Affected Areas:** service, refactor, infrastructure, technology, security, messaging

## Related Files

- `backend/authentication/.gitattributes` (+0/-0)
- `backend/authentication/.gitignore` (+0/-0)
- `.../.mvn/wrapper/maven-wrapper.properties` (+0/-0)
- `backend/authentication/mvnw` (+0/-0)
- `backend/authentication/mvnw.cmd` (+0/-0)
- `backend/auth/.gitattributes` (+0/-0)
- `backend/auth/.gitignore` (+0/-0)
- `backend/auth/.mvn/wrapper/maven-wrapper.properties` (+0/-0)
- `backend/auth/mvnw` (+0/-0)
- `backend/auth/mvnw.cmd` (+0/-0)

