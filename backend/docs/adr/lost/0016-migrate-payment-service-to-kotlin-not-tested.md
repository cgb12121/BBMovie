# 16. - migrate payment service to kotlin (not tested)

**Status:** Proposed  
**Date:** 2025-08-08 (Decision window: 11 days)  
**Deciders:** Cao Gia Bao  
**Commit:** f81631af (12 commits in window)

## Context

The decision to decompose the monolithic file-service into three specialized microservices: media-service, media-upload-service, and media-streaming-service was driven by velocity in development, security compliance requirements, cost optimization, and resource efficiency. The event-driven communication via NATS JetStream replaced direct Kafka usage due to project's design boundaries.

**Commit Message:** - migrate payment service to kotlin (not tested)




**Decision Window:** This ADR represents 12 related commits over 11 days:
- 30c82cbf: - migrate refresh token control from using email+user-agent 
- fe90bbd7: - migrate refresh token control from using email+user-agent 
- b07218b5: - create new light way blacklist token filter for gateway to
- 6e9280ba: - update experimental approach to prevent replay attack
- a8334091: - update totp
... and 7 more commits

## Decision

The file-service was decomposed into three specialized microservices: media-service for metadata management and source of truth, media-upload-service for upload presign URLs and file handling, and media-streaming-service for media streaming. The decision was made based on the need for better maintainability, security, cost optimization, and resource efficiency.

## Consequences

- Positive: Improved code maintainability
- Positive: Enhanced security and compliance requirements
- Negative: Potential trade-off between development velocity and code complexity
- Negative: Increased resource consumption due to decomposed services
## Alternatives Considered

- Alternative 1: Keeping the file-service as a monolithic 'god service'
- Alternative 2: Decomposing into multiple microservices but using Kafka as messaging

## Technical Details

- **Commit Hash:** `f81631afcc0d6cfab3b8e7977ea87b9b1d42d408`
- **Files Changed:** 10
- **Lines Added:** 11129
- **Lines Removed:** 0
- **Affected Areas:** refactor, service, infrastructure, security, technology, messaging, database

## Related Files

- `.../com/bbmovie/auth/config/KafkaTopicConfig.java` (+0/-0)
- `.../bbmovie/auth/controller/AuthController.java` (+0/-0)
- `.../main/java/com/bbmovie/auth/entity/User.java` (+0/-0)
- `.../auth/repository/RefreshTokenRepository.java` (+0/-0)
- `.../java/com/bbmovie/auth/security/CorsConfig.java` (+0/-0)
- `.../security/jose/JoseAuthenticationFilter.java` (+0/-0)
- `.../auth/security/jose/JoseValidatedToken.java` (+0/-0)
- `.../auth/security/jose/config/JoseConstraint.java` (+0/-0)
- `.../auth/security/jose/jwt/io/JwtioAsymmetric.java` (+0/-0)
- `.../auth/security/jose/jwt/io/JwtioSymmetric.java` (+0/-0)

