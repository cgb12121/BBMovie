# 30. - add endpoint to support light way routing for gateway (jwe)

**Status:** Proposed  
**Date:** 2025-09-07 (Decision window: 2 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 554006cb (3 commits in window)

## Context

The decision to add an endpoint for light way routing in the gateway service was driven by the need to improve development velocity and maintain code quality while adhering to security compliance requirements. The monolithic file-service had become cumbersome, hindering efficient file operations and preventing seamless integration with modern technologies such as reactive programming and NATS JetStream.

**Commit Message:** - add endpoint to support light way routing for gateway (jwe)




**Decision Window:** This ADR represents 3 related commits over 2 days:
- 4842d869: - add endpoint to support light way routing for gateway (jwe
- 554006cb: - add endpoint to support light way routing for gateway (jwe
- e6a0a0ce: - add endpoint to support light way routing for gateway (jwe

## Decision

To address this issue, a new endpoint was added in the gateway service to support light way routing, allowing for better routing decisions based on request characteristics. This decision was made to leverage the benefits of reactive programming and enhance overall system performance while maintaining security standards.

## Consequences

- Positive: Improved development velocity and maintainability.
- Positive: Enhanced system performance through better routing decisions.
- Negative: Possible trade-off in terms of increased complexity for developers.
- Negative: Risk of introducing potential security vulnerabilities if not properly implemented.
## Alternatives Considered

- Alternative 1: Keeping the monolithic file-service architecture, which would have resulted in slower development velocity and difficulty in integrating modern technologies.
- Alternative 2: Decomposing the file-service into multiple microservices in a single step, which could lead to an increase in technical debt.

## Technical Details

- **Commit Hash:** `554006cb5aa2e7f9c8bcc6e1dd4d9de6eee5cf96`
- **Files Changed:** 10
- **Lines Added:** 1268
- **Lines Removed:** 0
- **Affected Areas:** service, infrastructure

## Related Files

- `.../com/bbmovie/auth/controller/JweController.java` (+0/-0)
- `.../auth/security/jose/JoseProviderStrategy.java` (+0/-0)
- `.../security/jose/JoseProviderStrategyContext.java` (+0/-0)
- `.../auth/security/jose/jwt/io/JwtioAsymmetric.java` (+0/-0)
- `.../auth/security/jose/jwt/io/JwtioSymmetric.java` (+0/-0)
- `.../com/bbmovie/auth/controller/JwkController.java` (+0/-0)
- `.../com/bbmovie/gateway/config/CorsConfig.java` (+0/-0)
- `.../bbmovie/gateway/config/RateLimiterConfig.java` (+0/-0)
- `.../bbmovie/gateway/security/AuthHeaderFilter.java` (+0/-0)
- `.../bbmovie/payment/config/JsonObjectMapper.java` (+0/-0)

