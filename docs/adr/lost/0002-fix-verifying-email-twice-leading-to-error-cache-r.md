# 2. Fix verifying email twice leading to error (cache removed the token email), fix 

**Status:** Accepted  
**Date:** 2025-04-19 (Decision window: 2 days)  
**Deciders:** Cao Gia Bao  
**Commit:** a1a56de0 (4 commits in window)

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices: 1. **media-service** - Metadata management and source of truth 2. **media-upload-service** - Upload presign URLs and file handling 3. **transcode-worker** - CPU-intensive transcoding tasks to optimize cost and resource efficiency. Security and compliance requirements necessitated this architectural change.

**Commit Message:** Fix verifying email twice leading to error (cache removed the token email), fix loggin error by loading actual email.




**Decision Window:** This ADR represents 4 related commits over 2 days:
- a1a56de0: Fix verifying email twice leading to error (cache removed th
- 73c9f704: Fix csrf provider, fix login, register on frontend. add elas
- 8a672e54: nothing
- 40583755: nothing

## Decision

The transcode-worker service was decomposed from the legacy 'file-service' to handle CPU-intensive transcoding tasks separately, allowing for better security and compliance, as well as cost optimization and improved resource efficiency.

## Consequences

- Positive: Decoupling services leads to better scalability and easier maintenance.
- Positive: Improved security by isolating critical components.
- Negative: Trade-off - Initial migration effort required.
- Negative: Risk - Potential disruption during the decomposing process.
## Alternatives Considered

- Alternative 1: Keeping 'file-service' monolithic as it was
- Alternative 2: Decompose all services together

## Technical Details

- **Commit Hash:** `a1a56de03b71c3555cf1adc36ace3b2dfd4bef8e`
- **Files Changed:** 10
- **Lines Added:** 5750
- **Lines Removed:** 0
- **Affected Areas:** refactor, infrastructure, technology

## Related Files

- `.../com/example/bbmovie/config/CorsConfig.java` (+0/-0)
- `.../com/example/bbmovie/config/RedisConfig.java` (+0/-0)
- `.../example/bbmovie/controller/AuthController.java` (+0/-0)
- `.../controller/advice/GlobalExceptionHandler.java` (+0/-0)
- `.../example/bbmovie/security/JwtTokenProvider.java` (+0/-0)
- `.vscode/settings.json` (+0/-0)
- `backend/.gitignore` (+0/-0)
- `backend/pom.xml` (+0/-0)
- `.../bbmovie/audit/RequestLoggingFilter.java` (+0/-0)
- `.../example/bbmovie/common/ValidationHandler.java` (+0/-0)

