# 22. - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and 

**Status:** Proposed  
**Date:** 2025-08-11  
**Deciders:** Cao Gia Bao  
**Commit:** fe5446bd

## Context

The decision to support both v1 and v2 Zalopay payment methods was needed to enhance development velocity and maintain code maintainability. The BBMovie platform is adopting a microservices architecture style, which requires supporting multiple payment gateways for seamless integration and flexibility in the future.

**Commit Message:** - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and POST via JSON (zalopay) callback





## Decision

The `PaymentController.java` was updated to support both v1 and v2 Zalopay payment methods via HTTP GET via params (vnpay) and POST via JSON (zalopay). The `PaymentRequestDto.java`, `PaymentVerification.java`, `PaymentRequest.java`, and `RefundRequest.java` files were also modified to accommodate these changes.

## Consequences

- Positive: Enhanced development velocity due to support for multiple payment gateways
- Positive: Improved code maintainability with clear separation between v1 and v2 Zalopay payment methods
## Alternatives Considered

- Alternative 1: Not supporting both payment methods would have hindered the platform's ability to integrate new payment gateways in the future, leading to a decrease in development velocity.
- Alternative 2: Using a single payment gateway for all transactions could have negatively affected code maintainability and flexibility.

## Technical Details

- **Commit Hash:** `fe5446bddb902e6500c9ed197bbd3c97845ca08c`
- **Files Changed:** 5
- **Lines Added:** 556
- **Lines Removed:** 0
- **Affected Areas:** infrastructure

## Related Files

- `.../payment/controller/PaymentController.java` (+0/-0)
- `.../com/bbmovie/payment/dto/PaymentRequestDto.java` (+0/-0)
- `.../bbmovie/payment/dto/PaymentVerification.java` (+0/-0)
- `.../payment/dto/{ => request}/PaymentRequest.java` (+0/-0)
- `.../RefundRequest.java}` (+0/-0)

