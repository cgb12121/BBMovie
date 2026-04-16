# 27. - disable  csrf

**Status:** Proposed  
**Date:** 2025-08-16  
**Deciders:** Cao Gia Bao  
**Commit:** 500a1c4b

## Context

The decision to disable CSRF in the auth-service security configuration was needed due to SECURITY and compliance requirements. The monolithic 'file-service' had caused data flow inconsistencies after decomposition, leading to a decomposed architecture that required strict security measures.

**Commit Message:** - disable  csrf





## Decision

The CSRF protection has been disabled in the SecurityConfig.java file of the auth-service to reduce complexity and improve performance while maintaining the security standards.

## Consequences

- Positive: Simplified security configuration
- Negative: Increased exposure to Cross-Site Request Forgery (CSRF) attacks
## Alternatives Considered

- Alternative 1: Keeping CSRF protection enabled would have maintained security, but at a cost of increased complexity and potential performance issues.
- Alternative 2: Implementing an anti-CSRF token mechanism would address the risk, but this could introduce additional complexity to the system.

## Technical Details

- **Commit Hash:** `500a1c4b42ebf0dd47502e4fd034631306acd541`
- **Files Changed:** 1
- **Lines Added:** 1
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `backend/auth/src/main/java/com/bbmovie/auth/security/SecurityConfig.java` (+0/-0)

