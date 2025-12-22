# 1. Implement Redis caching for email verification tokens and update user authentica

**Status:** Accepted  
**Date:** 2025-04-05 (Decision window: 11 days)  
**Deciders:** Cao Gia Bao  
**Commit:** a358d111 (7 commits in window)

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices to address SECURITY: Security and compliance requirements. These new services are designed to handle file operations through media-upload-service using presigned URLs, ensuring a secure way of managing files.

**Commit Message:** feat: Implement Redis caching for email verification tokens and update user authentication




**Decision Window:** This ADR represents 7 related commits over 11 days:
- a358d111: feat: Implement Redis caching for email verification tokens 
- 95494cbe: Update project structure and add new features
- 05500a08: Update project structure and add new features
- c0d231d5: Update login, register pages
- 6dad27d1: Update login, register pages with animation
... and 2 more commits

## Decision

The BBMovie platform decided to retire the legacy 'file-service' and decompose its responsibilities into the 'media-upload-service', which now handles file operations via presigned URLs. This decision was made to improve security compliance and prevent direct database access between services.

## Consequences

- Positive: Enhances security and complies with regulatory requirements
- Positive: Reduces risk of data breaches by managing files through a controlled service
- Negative: Requires migration of some existing codebases and infrastructure components
- Negative: Introduces complexity in the system as new services are integrated
## Alternatives Considered

- Alternative 1: Retain the 'file-service' to handle file operations directly, which would not improve security compliance.
- Alternative 2: Decompose file-service into multiple microservices without using presigned URLs, increasing the risk of data breaches.

## Technical Details

- **Commit Hash:** `a358d11171dab16224ccfa4de6f8735a21c69aab`
- **Files Changed:** 10
- **Lines Added:** 35927
- **Lines Removed:** 0
- **Affected Areas:** security, infrastructure, technology

## Related Files

- `backend/.gitattributes` (+0/-0)
- `backend/.gitignore` (+0/-0)
- `backend/.mvn/wrapper/maven-wrapper.properties` (+0/-0)
- `backend/mvnw` (+0/-0)
- `backend/mvnw.cmd` (+0/-0)
- `.vscode/settings.json` (+0/-0)
- `backend/pom.xml` (+0/-0)
- `.../com/example/bbmovie/common/BlindResult.java` (+0/-0)
- `.../example/bbmovie/common/ValidationHandler.java` (+0/-0)
- `frontend/package-lock.json` (+0/-0)

