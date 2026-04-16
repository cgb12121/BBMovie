# 14. - update role based access control to attribute based access control

**Status:** Proposed  
**Date:** 2025-07-27 (Decision window: 1 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 204d0c68 (2 commits in window)

## Context

The decision to update role-based access control (RBAC) to attribute-based access control (ABAC) in the BBMovie platform was needed due to increasing security requirements and compliance demands. The monolithic file-service's lack of scalability and maintainability hindered development velocity, making an ABAC implementation necessary.

**Commit Message:** - update role based access control to attribute based access control

**Details:** Signed-off-by: Cao Gia Bao <148226446+cgb12121@users.noreply.github.com>


**Decision Window:** This ADR represents 2 related commits over 1 days:
- 937618d7: - add missing security config annotation
- 204d0c68: - update role based access control to attribute based access

## Decision

The BBMovie platform has decided to migrate its role-based access control system from Java Spring Security RBAC configuration in `SecurityConfig.java` to attribute-based access control (ABAC) using OpenID Connect. This decision was made for better security and compliance requirements as well as improved maintainability of the codebase.

## Consequences

- Positive: Improved security and compliance with evolving standards
- Positive: Enhanced maintainability and scalability of the codebase, allowing for faster development velocity
- Negative: Requires refactoring current RBAC configuration to fit ABAC implementation
- Negative: Potential risk of disrupting existing services due to changes in access control mechanisms
## Alternatives Considered

- Alternative 1: Staying with RBAC without considering the security and compliance requirements.
- Alternative 2: Implementing ABAC from scratch, which would have been time-consuming and resource-intensive.

## Technical Details

- **Commit Hash:** `204d0c68bba8897b0062dec75e88cc3cf7dacc31`
- **Files Changed:** 6
- **Lines Added:** 511
- **Lines Removed:** 0
- **Affected Areas:** infrastructure, security, technology, service

## Related Files

- `.../src/main/java/com/bbmovie/fileservice/sercurity/SecurityConfig.java` (+0/-0)
- `backend/auth/pom.xml` (+0/-0)
- `.../main/java/com/bbmovie/auth/entity/User.java` (+0/-0)
- `.../bbmovie/auth/entity/enumerate/Permission.java` (+0/-0)
- `.../com/bbmovie/auth/entity/enumerate/Region.java` (+0/-0)
- `.../com/bbmovie/auth/entity/enumerate/Role.java` (+0/-0)

