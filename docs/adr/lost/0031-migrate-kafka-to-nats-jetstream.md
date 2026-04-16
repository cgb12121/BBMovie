# 31. - migrate kafka to nats jetstream

**Status:** Proposed  
**Date:** 2025-09-16 (Decision window: 9 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 92a2c88d (11 commits in window)

## Context

The decision to migrate Kafka to NATS JetStream was needed to improve development velocity and code maintainability. The event-driven communication via NATS JetStream aligns with the microservices architecture style and design boundaries. It eliminates direct DB access between services and enables non-blocking I/O using reactive programming with WebFlux.

**Commit Message:** - migrate kafka to nats jetstream




**Decision Window:** This ADR represents 11 related commits over 9 days:
- 5f3094f6: - refractor for provider expected response
- b6f7c8c5: - refractor for provider expected response
- 718cf892: - refractor for provider expected response
- face2be3: - refractor for provider expected response
- 76742e5a: - refractor for provider expected response
... and 6 more commits

## Decision

The BBMovie platform decided to migrate Kafka to NATS JetStream for better performance, scalability, fault tolerance, and security requirements. The migration aims to reduce the complexity of the monolithic 'file-service' into specialized microservices like media-upload-service and media-streaming-service.

## Consequences

- Positive: Improved development velocity and maintainability
- Positive: Better performance and scalability requirements
- Negative: Trade-off between maintaining backward compatibility and adopting new technologies
- Negative: Potential risk of data loss or inconsistency during migration
## Alternatives Considered

- Alternative 1: Staying with Kafka for its established ecosystem and support
- Alternative 2: Migrating to Apache Pulsar due to its scalability and fault tolerance features

## Technical Details

- **Commit Hash:** `92a2c88d9d7089a5b5dcb0f7b5f1fdda0e52e6ce`
- **Files Changed:** 10
- **Lines Added:** 20925
- **Lines Removed:** 0
- **Affected Areas:** refactor, infrastructure, technology, messaging, service

## Related Files

- `.../com/bbmovie/payment/PaymentApplication.java` (+0/-0)
- `.../payment/controller/PaymentController.java` (+0/-0)
- `.../controller/SubscriptionPlanController.java` (+0/-0)
- `.../payment/dto/request/PaymentRequest.java` (+0/-0)
- `.../bbmovie/payment/dto/request/RefundRequest.java` (+0/-0)
- `.../com/bbmovie/payment/config/MessageConfig.java` (+0/-0)
- `.../bbmovie/payment/entity/PaymentTransaction.java` (+0/-0)
- `.../payment/entity/enums/PaymentStatus.java` (+0/-0)
- `.../com/bbmovie/payment/service/I18nService.java` (+0/-0)
- `.../openapi => config}/OpenApiConfig.java` (+0/-0)

