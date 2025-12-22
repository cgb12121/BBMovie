# 40. - debug(ai-service) add security to the service

**Status:** Proposed  
**Date:** 2025-10-24 (Decision window: 1 days)  
**Deciders:** Cao Gia Bao  
**Commit:** fa00f20b (6 commits in window)

## Context

The decision to add security to the ai-service was needed due to increasing compliance requirements and concerns over sensitive user data. As per the project's microservices architecture, directly integrating security features into each service is crucial for maintaining a secure environment.

**Commit Message:** - debug(ai-service) add security to the service




**Decision Window:** This ADR represents 6 related commits over 1 days:
- 17434c92: - debug(auth) add admin token generation util for testing
- b812ef18: - debug(auth) add admin token generation util for testing
- fa00f20b: - debug(ai-service) add security to the service
- 8d9dc67e: - refractor(ai-service): small changes in logging behaviour 
- babc9da5: refractor(ai-service): enhance session handling, update repo
... and 1 more commits

## Decision

To enhance security, the ai-service now includes JWT (JSON Web Token) validation in its AdminTokenGenerator class, which verifies the token's signature and expiration date before processing requests. This decision was made to prevent unauthorized access to sensitive user data.

## Consequences

- Positive: Improved security for handling sensitive user data.
- Negative: Increased complexity of the ai-service codebase due to additional security validations.
## Alternatives Considered

- Alternative 1: Not implementing JWT validation directly within the ai-service, instead opting for a centralized authentication and authorization service.
- Alternative 2: Not addressing security concerns in this specific microservice, but rather focusing on improving other areas of the system.

## Technical Details

- **Commit Hash:** `fa00f20b339bd017613ff50a888f2255b8fa7d58`
- **Files Changed:** 10
- **Lines Added:** 990
- **Lines Removed:** 0
- **Affected Areas:** security, service, technology, infrastructure, refactor, database, messaging

## Related Files

- `.../java/com/bbmovie/auth/entity/jose/JoseKey.java` (+0/-0)
- `.../service/student/UniversityRegistryService.java` (+0/-0)
- `.../bbmovie/auth/utils/AdminTokenGenerator.java` (+0/-0)
- `.../src/main/resources/META-INF/persistence.xml` (+0/-0)
- `.../auth/src/main/java/com/bbmovie/auth/utils/AdminTokenGenerator.java` (+0/-0)
- `backend/ai-assistant-service/pom.xml` (+0/-0)
- `.../ai_assistant_service/agent/AdminAssistant.java` (+0/-0)
- `.../ai_assistant_service/agent/UserAssistant.java` (+0/-0)
- `.../agent/tool/AdminTools.java` (+0/-0)
- `.../agent/tool/GuestTools.java` (+0/-0)

