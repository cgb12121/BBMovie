# 3. Migrate from symmetric jwt to asymmetric jwt

**Status:** Accepted  
**Date:** 2025-05-03 (Decision window: 26 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 655e290b (21 commits in window)

## Context

The BBMovie platform needed to enhance system reliability and fault tolerance, as well as comply with security and compliance requirements after decomposing the monolithic 'file-service' into three specialized microservices: media-service, media-upload-service, and media-streaming-service. The decision was driven by development velocity and code maintainability, as well as performance and scalability requirements.

**Commit Message:** migrate from symmetric jwt to asymmetric jwt




**Decision Window:** This ADR represents 21 related commits over 26 days:
- b40e0e2e: update jwt filter to prevent query db, add retry and sleep f
- d04b7674: update jwt filter to prevent query db, add retry and sleep f
- f4739099: Add Local MiniLM to generate vector embedding instead of rel
- 5c6aef3f: temporary fix the elasticsearch...
- 76b4ed84: feat<Cloudinary> creates api to handle upload/delete operati
... and 16 more commits

## Decision

The BBMovie platform migrated from a symmetric JWT to an asymmetric JWT for authentication purposes in the auth-service (Spring Boot) and gateway (Spring Boot). This decision was made to improve security and comply with compliance requirements.

## Consequences

- Positive: Enhanced security and compliance
- Positive: Improved system reliability and fault tolerance
- Negative: Increased complexity due to additional dependencies and integration
- Negative: Potential risk in managing asymmetric JWTs
## Alternatives Considered

- Staying with symmetric JWTs for better development velocity and code maintainability
- Using a different authentication method such as OAuth2.0 or OpenID Connect

## Technical Details

- **Commit Hash:** `655e290bd4ea69ca39877d66c7ed19ec6eb8547b`
- **Files Changed:** 10
- **Lines Added:** 12335
- **Lines Removed:** 0
- **Affected Areas:** service, database, messaging, security, technology, infrastructure, refactor, architecture

## Related Files

- `backend/pom.xml` (+0/-0)
- `.../java/com/example/bbmovie/config/AIConfig.java` (+0/-0)
- `.../example/bbmovie/config/RestTemplateConfig.java` (+0/-0)
- `.../example/bbmovie/config/VectorStoreConfig.java` (+0/-0)
- `.../example/bbmovie/controller/AuthController.java` (+0/-0)
- `.vscode/settings.json` (+0/-0)
- `frontend/package-lock.json` (+0/-0)
- `.../bbmovie/controller/MovieController.java` (+0/-0)
- `.../bbmovie/controller/SampleDataController.java` (+0/-0)
- `.../exception/advice/GlobalExceptionHandler.java` (+0/-0)

