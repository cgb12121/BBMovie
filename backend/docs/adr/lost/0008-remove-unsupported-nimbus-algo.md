# 8. Remove unsupported nimbus algo

**Status:** Proposed  
**Date:** 2025-07-15 (Decision window: 2 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 549804af (6 commits in window)

## Context

The decomposed file-service poses security and compliance risks due to its monolithic nature. As such, the BBMovie platform decided to migrate the media-upload-service, media-streaming-service, and transcode-worker from using unsupported Nimbus algorithms to using more secure implementations. This decision aligns with the project's architecture design boundaries that emphasize security and reliability.

**Commit Message:** remove unsupported nimbus algo




**Decision Window:** This ADR represents 6 related commits over 2 days:
- 610716fd: add more claims to increase security of jwt.
- 43b26a54: change artifact name of file service
- 5cb85315: remove unsupported nimbus algo
- b9d2cfe6: remove unsupported nimbus algo
- 549804af: remove unsupported nimbus algo
... and 1 more commits

## Decision

The BBMovie platform migrated the file-service's media-upload-service, media-streaming-service, and transcode-worker by replacing their reliance on unsupported Nimbus algorithms with more secure implementations such as jose4j. The migration not only addresses security concerns but also improves system maintainability and development velocity.

## Consequences

- Positive: Improved security and compliance
- Positive: Enhanced code maintainability and development velocity
- Negative: Potential trade-off in terms of short-term development effort due to the need for refactoring existing codebases
- Negative: Risk of introducing new vulnerabilities if not properly tested and audited
## Alternatives Considered

- Alternative 1: Keeping the unsupported Nimbus algorithms, which would have continued posing security risks to the system.
- Alternative 2: Not addressing the issue, resulting in a higher risk of data breaches and compliance violations.

## Technical Details

- **Commit Hash:** `549804afaf9de76d2dd76fccb61506009f244467`
- **Files Changed:** 10
- **Lines Added:** 5576
- **Lines Removed:** 0
- **Affected Areas:** security, refactor, technology, service, infrastructure

## Related Files

- `.../jose/jwe/nimbus/JweNimbusProvider.java` (+0/-0)
- `.../security/jose/jwk/encrypt/JwkJweProvider.java` (+0/-0)
- `.../security/jose/jwk/sign/JwkJwtProvider.java` (+0/-0)
- `.../security/jose/jwt/io/JoseHmacProvider.java` (+0/-0)
- `.../security/jose/jwt/io/JoseRsaProvider.java` (+0/-0)
- `.../.gitattributes` (+0/-0)
- `.../.gitignore` (+0/-0)
- `.../.mvn/wrapper/maven-wrapper.properties` (+0/-0)
- `backend/{bbmovie-upload-file => api-gateway}/mvnw` (+0/-0)
- `.../{bbmovie-upload-file => api-gateway}/mvnw.cmd` (+0/-0)

