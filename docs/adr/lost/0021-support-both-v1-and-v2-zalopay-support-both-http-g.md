# 21. - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and 

**Status:** Proposed  
**Date:** 2025-08-10  
**Deciders:** Cao Gia Bao  
**Commit:** 17cef1dc

## Context

The decision to support both v1 and v2 Zalopay payment methods was driven by development velocity and maintainability concerns. The monolithic 'file-service' had become cumbersome, leading to code duplication and increased complexity in the payment-service. Decomposing it into specialized microservices such as media-upload-service and payment-service allowed for better separation of concerns and facilitated easier maintenance.

**Commit Message:** - support both v1 and v2 zalopay - support both HTTP GET via params (vnpay) and POST via JSON (zalopay) callback





## Decision

The decision was made to support both v1 and v2 Zalopay payment methods by introducing ZaloPayAdapter.java and VnpayAdapter.java in the payment-service. This allows handling HTTP GET via params (vnpay) and POST via JSON (zalopay) callbacks separately, reducing code duplication and making it easier to maintain.

## Consequences

- Positive: Improved maintainability and reduced complexity.
- Positive: Better separation of concerns between payment methods.
- Negative: Increased codebase size due to additional adapters.
- Negative: Potential risk of inconsistencies if not properly tested.
## Alternatives Considered

- Alternative 1: Keeping the payment method handling in a single controller, which would have led to increased complexity and harder maintenance.
- Alternative 2: Extending the existing ZaloPayAdapter.java for vnpay, which would have resulted in code duplication and reduced maintainability.

## Technical Details

- **Commit Hash:** `17cef1dc4c9788093dabf85851996dba6a44b3c0`
- **Files Changed:** 5
- **Lines Added:** 270
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `.../payment/controller/PaymentController.java` (+0/-0)
- `.../payment/exception/NormalizeDataException.java` (+0/-0)
- `.../payment/service/vnpay/VnpayAdapter.java` (+0/-0)
- `.../payment/service/zalopay/ZaloPayAdapter.java` (+0/-0)
- `.../payment/service/zalopay/ZaloPayConstraint.java` (+0/-0)

