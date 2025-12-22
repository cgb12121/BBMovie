# 29. - check transaction expire to prevent replay

**Status:** Proposed  
**Date:** 2025-08-18  
**Deciders:** Cao Gia Bao  
**Commit:** beaaf9a2

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices to enhance development velocity and code maintainability. The pressure signals driving this decision include VELOCITY for maintaining high development pace and SECURITY for meeting compliance requirements.

**Commit Message:** - check transaction expire to prevent replay





## Decision

The media-upload-service was introduced to handle file upload operations, replacing the direct interaction with the decomposed file-service. This decision aims to streamline the data flow, prevent potential inconsistencies after service decomposition, and ensure secure communication between services.

## Consequences

- Positive: Improved maintainability of codebase by breaking down monolithic architecture into microservices
- Positive: Enhanced security compliance due to separation of concerns and better control over access rights
- Negative: Potential trade-off in development time due to the need for refactoring existing code and setting up new services
- Negative: Risk of data inconsistency if not properly managed during transition, as some services might rely on outdated data from the decomposed file-service
## Alternatives Considered

- Alternative 1: Keeping the monolithic 'file-service' to simplify development process
- Alternative 2: Relying solely on NATS JetStream for event-driven communication between microservices, without considering alternative messaging platforms like Kafka or RabbitMQ for better scalability and fault tolerance

## Technical Details

- **Commit Hash:** `beaaf9a216ddef9b5d6cb8c018f1a994acf07cab`
- **Files Changed:** 5
- **Lines Added:** 99
- **Lines Removed:** 0
- **Affected Areas:** messaging

## Related Files

- `.../bbmoviesearch/security/SecurityConfig.java` (+0/-0)
- `.../bbmovie/payment/exception/MomoException.java` (+0/-0)
- `.../exception/TransactionExpiredException.java` (+0/-0)
- `.../bbmovie/payment/service/momo/MomoAdapter.java` (+0/-0)
- `.../payment/service/paypal/PayPalAdapter.java` (+0/-0)

