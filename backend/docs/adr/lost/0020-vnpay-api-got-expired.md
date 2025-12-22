# 20. - Vnpay api got expired

**Status:** Proposed  
**Date:** 2025-08-09 (Decision window: 1 days)  
**Deciders:** Cao Gia Bao  
**Commit:** c4a400c6 (6 commits in window)

## Context

The decision to migrate the file-service into three specialized microservices - media-upload-service, media-service, and transcode-worker - was needed due to the project's velocity and code maintainability pressure signals. The monolithic 'file-service' had become a bottleneck in development and required refactoring for better architecture and scalability.

**Commit Message:** - Vnpay api got expired




**Decision Window:** This ADR represents 6 related commits over 1 days:
- 8d1c8153: - add support for momo and zalopay
- c4a400c6: - Vnpay api got expired
- 31204297: - fix payment with vnpay
- f1aac38a: - fix payment with vnpay
- 63eb748a: - fix payment with vnpay
... and 1 more commits

## Decision

The BBMovie platform decomposed the legacy 'file-service' into three specialized microservices: media-upload-service, media-service, and transcode-worker to enhance code maintainability and handle file operations more efficiently. The new architecture promotes a service-oriented design with clear boundaries and responsibilities.

## Consequences

- Positive: Improved development velocity
- Positive: Better maintenance and scalability
- Negative: Potential trade-off in learning curve for the new microservices architecture
- Negative: Risk of losing familiarity with the old codebase
## Alternatives Considered

- Alternative 1: Keeping the monolithic 'file-service' to avoid refactoring and maintain familiarity with the codebase
- Alternative 2: Decomposing into a single service, media-upload-service, to simplify the migration process

## Technical Details

- **Commit Hash:** `c4a400c6264f9ff5f6081d340b5038960e89aa4f`
- **Files Changed:** 10
- **Lines Added:** 6048
- **Lines Removed:** 0
- **Affected Areas:** technology, infrastructure, service

## Related Files

- `.../controller/FileUploadController.java` (+0/-0)
- `backend/payment/pom.xml` (+0/-0)
- `.../payment/controller/PaymentController.kt` (+0/-0)
- `.../payment/service/payment/momo/MomoAdapter.kt` (+0/-0)
- `.../payment/service/payment/momo/MomoConstraint.kt` (+0/-0)
- `.../com/bbmovie/payment/PaymentApplication.java` (+0/-0)
- `.../bbmovie/payment/config/JpaAuditingConfig.java` (+0/-0)
- `.../payment/controller/PaymentController.java` (+0/-0)
- `.../java/com/bbmovie/payment/controller/Test.java` (+0/-0)
- `.../bbmovie/payment/entity/PaymentTransaction.java` (+0/-0)

