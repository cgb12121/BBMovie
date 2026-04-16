# 18. - integrate new auth mechanism to front end

**Status:** Proposed  
**Date:** 2025-08-09  
**Deciders:** Cao Gia Bao  
**Commit:** b232c3ed

## Context

The decision to integrate a new auth mechanism to the front end was needed to enhance security and compliance requirements as well as improve development velocity and code maintainability in the microservices architecture style of BBMovie platform.

The decision to decompose the monolithic file-service into three specialized microservices: media-service for metadata management and source of truth, media-upload-service for upload presign URLs and file handling, and transcode-worker for CPU-intensive tasks such as transcoding was driven by the need to improve development velocity and code maintainability. This change also addressed security and compliance requirements, as evidenced by the migration from Cloudinary's legacy ser...   

**Commit Message:** - integrate new auth mechanism to front end





## Decision

The `media-upload-service` was integrated with a new authentication mechanism, specifically JWK-based authentication, to replace the previous JWT-based auth mechanism. This decision was made to align with current security standards and requirements, while also providing better support for multi-factor authentication (MFA).

## Consequences

- Positive: Enhanced security and compliance
- Positive: Improved MFA capabilities
- Negative: Potential trade-off in performance due to additional network round-trips during the authentication process
## Alternatives Considered

- Alternative 1: Keeping the JWT-based auth mechanism, which might have been considered less secure and not compliant with current standards.
- Alternative 2: Implementing a token-based auth mechanism (like OAuth2) instead of JWK-based authentication. However, this alternative was discarded due to its complexity and potentially higher performance overhead.

## Technical Details

- **Commit Hash:** `b232c3edd6b3a16b8c6fc347e08863bef7cc0eda`
- **Files Changed:** 5
- **Lines Added:** 699
- **Lines Removed:** 0
- **Affected Areas:** security, technology, infrastructure

## Related Files

- `.../com/bbmovie/auth/controller/JwkController.java` (+0/-0)
- `.../com/bbmovie/auth/controller/MfaController.java` (+0/-0)
- `backend/payment/pom.xml` (+0/-0)
- `.../payment/controller/PaymentController.kt` (+0/-0)
- `.../com/bbmovie/payment/entity/base/BaseEntity.kt` (+0/-0)

