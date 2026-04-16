# 17. - migrate payment service to kotlin (not tested)

**Status:** Proposed  
**Date:** 2025-08-08  
**Deciders:** Cao Gia Bao  
**Commit:** 79630916

## Context

Migrate the payment service to Kotlin to improve maintainability and scalability in order to keep up with evolving business requirements and faster development cycles.

**Commit Message:** - migrate payment service to kotlin (not tested)





## Decision

The decision to migrate the payment-service from Java to Kotlin was made due to its benefits on code readability, performance, and developer productivity. Kotlin's concise syntax and type inference reduce boilerplate code, making it easier for developers to understand and maintain the codebase. Additionally, Kotlin's first-class support for coroutines enables non-blocking I/O operations, promoting scalability.

## Consequences

- Positive: Improved code quality and maintainability.
- Positive: Faster development cycles due to improved developer productivity.
- Negative: Potential learning curve for existing Java developers.
- Negative: Some performance impact may arise during the migration process.
## Alternatives Considered

- Keeping the payment-service in Java
- Considering other alternatives such as Rust or Go, but Kotlin was chosen because of its extensive support and good ecosystem.

## Technical Details

- **Commit Hash:** `79630916aa9b78fa6f52fd9e62d34faf7ca69680`
- **Files Changed:** 2
- **Lines Added:** 51
- **Lines Removed:** 0
- **Affected Areas:** service, refactor

## Related Files

- `.../payment/controller/PaymentController.kt` (+0/-0)
- `.../service/payment/dto/PaymentRequestDto.kt` (+0/-0)

