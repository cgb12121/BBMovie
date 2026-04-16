# 28. - disable  csrf

**Status:** Proposed  
**Date:** 2025-08-18  
**Deciders:** Cao Gia Bao  
**Commit:** 6bf09a59

## Context

The decision to disable CSRF in the BBMovie platform was needed due to security compliance requirements, as file operations must go through media-upload-service for presigned URLs. This migration enhances security and reduces potential risks.

**Commit Message:** - disable  csrf





## Decision

The `csrf` disabling strategy has been implemented to enhance security and comply with compliance requirements. The Spring Security CorsConfig class was updated to disable CSRF protection globally.

## Consequences

- Positive: Improved security
- Positive: Reduced potential risks
## Alternatives Considered

- Alternative 1: Not disabling CSRF, keeping it enabled for all requests, which might have increased the risk of Cross-Site Request Forgery attacks and violated compliance requirements.
- Alternative 2: Disabling CSRF only for specific endpoints, but this would require a more granular approach to security and could be complex.

## Technical Details

- **Commit Hash:** `6bf09a59fa59ab531c0c7f6402bf23518e70974f`
- **Files Changed:** 5
- **Lines Added:** 84
- **Lines Removed:** 0
- **Affected Areas:** infrastructure

## Related Files

- `.../bbmovie/auth/controller/AuthController.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/entity/User.java` (+0/-0)
- `.../java/com/bbmovie/auth/security/CorsConfig.java` (+0/-0)
- `.../bbmoviesearch/security/SecurityConfig.java` (+0/-0)
- `.../controller/FileStreamingController.java` (+0/-0)

