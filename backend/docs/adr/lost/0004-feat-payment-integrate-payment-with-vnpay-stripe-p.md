# 4. Feat<payment> integrate payment with Vnpay, Stripe, Paypal

**Status:** Accepted  
**Date:** 2025-05-31 (Decision window: 9 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 44f67a33 (5 commits in window)

## Context

The decision to decompose the monolithic file-service into media-upload-service, media-streaming-service, and media-service was driven by the need for increased development velocity and code maintainability. As file-service had become a bloated 'god service', its decomposition allowed for more modularized and scalable microservices architecture in accordance with BBMovie's project design boundaries.

**Commit Message:** feat<payment> integrate payment with Vnpay, Stripe, Paypal




**Decision Window:** This ADR represents 5 related commits over 9 days:
- 08c374ac: - refractor code for maintainability SonarQube
- 44f67a33: feat<payment> integrate payment with Vnpay, Stripe, Paypal
- d29c8fb7: [Major] <br> - update DJL AI from using Cpu to Gpu.
- 967e58f1: [Major] <br> - update DJL AI from using Cpu to Gpu.
- bc1f813f: [Fix] - Fix OAuth2 authentication mismatch fields when creat

## Decision

The BBMovie platform decomposed the monolithic `file-service` into three specialized microservices: 1) **media-upload-service** - Upload presign URLs and file handling, 2) **media-streaming-service** - Streaming media files, and 3) **media-service** - Metadata management and source of truth. This was done to improve code maintainability, security, and compliance requirements.

## Consequences

- Positive: Better code organization leading to easier maintenance
- Positive: Improved security and compliance by separating concerns into specialized services
- Negative: Potential trade-off is the need for additional communication between microservices through APIs or events
- Negative: Risk of increased complexity due to the introduction of new microservices
## Alternatives Considered

- Alternative 1: Keeping the file-service monolithic, but refactoring it, which would have led to a less modularized architecture and slower development velocity
- Alternative 2: Decomposing the file-service into multiple services before decomposing it further. However, this approach would not address the need for increased security and compliance

## Technical Details

- **Commit Hash:** `44f67a33eb0ed801c5fc86f97775d90a962943d0`
- **Files Changed:** 10
- **Lines Added:** 4764
- **Lines Removed:** 0
- **Affected Areas:** refactor, technology, infrastructure, security

## Related Files

- `backend/pom.xml` (+0/-0)
- `.../example/bbmovie/config/VectorStoreConfig.java` (+0/-0)
- `.../bbmovie/constant/AuthErrorMessages.java` (+0/-0)
- `...rMessages.java => CloudinaryErrorMessages.java}` (+0/-0)
- `.../com/example/bbmovie/constant/MimeType.java` (+0/-0)
- `.../bbmovie/controller/PaymentController.java` (+0/-0)
- `.../bbmovie/dto/request/PaymentRequestDto.java` (+0/-0)
- `.../bbmovie/dto/request/RefundRequestDto.java` (+0/-0)
- `.../example/bbmovie/entity/PaymentTransaction.java` (+0/-0)
- `.../java/com/example/bbmovie/constant/Domain.java` (+0/-0)

