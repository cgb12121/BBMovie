# 23. - add feature to discount student

**Status:** Proposed  
**Date:** 2025-08-13 (Decision window: 2 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 40d9672c (5 commits in window)

## Context

The decision to decompose the monolithic file-service into three specialized microservices - media-service, media-upload-service, and transcode-worker - was driven by the need for increased development velocity, code maintainability, and adherence to security and compliance requirements. As the BBMovie platform scaled, the monolithic 'god service' (file-service) became cumbersome and prone to bugs and security vulnerabilities. Therefore, restructuring the file-service into microservices facilitated better modularity, easier testing, and faster deployment.

**Commit Message:** - add feature to discount student




**Decision Window:** This ADR represents 5 related commits over 2 days:
- fc5c1b9e: - fix paypal request unable to return redirect to payment si
- 1816a0e8: - add callback and ipn endpoints
- 6bcbaf96: - add callback and ipn endpoints
- 65ac3bf1: - add callback and ipn endpoints
- 40d9672c: - add feature to discount student

## Decision

The BBMovie platform decided to decompose the legacy file-service into three specialized microservices: media-upload-service for presigned URLs and file uploads, transcode-worker for CPU-intensive tasks such as video transcoding, and media-service for metadata management. This decision was made to improve code maintainability, speed up development velocity, and ensure better security and compliance.

## Consequences

- Positive: Improved modularity of the system which allows for easier maintenance and updates
- Positive: Faster deployment cycles as the system becomes more modular and modular services can be deployed independently
- Negative: Potential increase in operational complexity due to increased number of microservices and external dependencies
- Negative: Increased risk of security vulnerabilities if not properly managed
## Alternatives Considered

- Keeping the monolithic file-service architecture, which would have led to potential code bloat, decreased maintainability, and slower deployment cycles
- Deploying a single AI service to handle all CPU-intensive tasks, which would have limited the BBMovie platform's ability to scale its AI processing power

## Technical Details

- **Commit Hash:** `40d9672c1a0b71affa63c8de56e95d7411816369`
- **Files Changed:** 10
- **Lines Added:** 83538
- **Lines Removed:** 0
- **Affected Areas:** service, infrastructure, technology

## Related Files

- `.../payment/controller/PaymentController.java` (+0/-0)
- `.../com/bbmovie/payment/dto/WebhookPayload.java` (+0/-0)
- `.../payment/exception/PayPalPaymentException.java` (+0/-0)
- `.../payment/service/PaymentProviderAdapter.java` (+0/-0)
- `.../bbmovie/payment/service/PaymentService.java` (+0/-0)
- `.../dto/request/CallbackRequestContext.java` (+0/-0)
- `.../dto/response/PaymentVerificationResponse.java` (+0/-0)
- `.../dto/response/PaymentCreationResponse.java` (+0/-0)
- `.../bbmovie/payment/service/momo/MomoAdapter.java` (+0/-0)
- `.../payment/service/paypal/PayPalAdapter.java` (+0/-0)

