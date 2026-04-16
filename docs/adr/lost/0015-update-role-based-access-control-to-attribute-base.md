# 15. - update role based access control to attribute based access control

**Status:** Proposed  
**Date:** 2025-07-27  
**Deciders:** Cao Gia Bao  
**Commit:** 4a78c10f

## Context

The decision to update role-based access control (RBAC) to attribute-based access control (ABAC) was driven by the need for enhanced security and compliance requirements in the BBMovie platform. The monolithic 'file-service' had become cumbersome, making it challenging to manage user permissions effectively. As such, decomposing this service into specialized microservices like `media-upload-service` and `media-service`, while implementing ABAC, became necessary to improve data flow consistency and maintain code maintainability.

**Commit Message:** - update role based access control to attribute based access control

**Details:** Signed-off-by: Cao Gia Bao <148226446+cgb12121@users.noreply.github.com>



## Decision

The BBMovie platform decided to migrate the 'file-service' (legacy monolithic system) into three new services: media-upload-service, media-service, and file-service. The decision was made to implement attribute-based access control instead of role-based access control to enhance security and compliance requirements.

## Consequences

- Positive: Improved security by using ABAC for fine-grained authorization
- Positive: Better compliance with GDPR data protection regulation
- Negative: Trade-off of increased complexity in managing user permissions
- Negative: Risk of delayed implementation due to the learning curve associated with ABAC
## Alternatives Considered

- Alternative 1: Staying with role-based access control for simplicity and faster implementation - However, this decision was not chosen as it would have compromised security and compliance requirements.
- Alternative 2: Implementing attribute-based access control from the start of development to enhance security and prevent future challenges - This approach was considered but deemed too costly in terms of time and resources compared to migrating after decomposing the monolithic 'file-service'.

## Technical Details

- **Commit Hash:** `4a78c10ff5bb7c2b7730f5bf15ffa8b4de32e37e`
- **Files Changed:** 5
- **Lines Added:** 715
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `.../auth/controller/admin/JoseDebugController.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/entity/User.java` (+0/-0)
- `.../java/com/bbmovie/auth/entity/jose/JwkKey.java` (+0/-0)
- `.../auth/security/jose/JoseProviderStrategy.java` (+0/-0)
- `.../auth/security/jose/config/JoseConstraint.java` (+0/-0)

