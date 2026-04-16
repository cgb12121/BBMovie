# 13. - fix routing error when gateway trying to forward request to itself

**Status:** Proposed  
**Date:** 2025-07-26  
**Deciders:** Cao Gia Bao  
**Commit:** a280fc0f

## Context

The routing error when gateway tries to forward request to itself in media-upload-service was needed due to the decomposed file-service architecture. The monolithic 'file-service' has been split into media-upload-service, media-streaming-service, and other specialized microservices. This migration aims to improve scalability, maintainability, and easier debugging of code.

**Commit Message:** - fix routing error when gateway trying to forward request to itself

**Details:** Signed-off-by: Cao Gia Bao <148226446+cgb12121@users.noreply.github.com>



## Decision

To fix this routing error, the gateway's route configuration for media-upload-service was updated in application.yml file. The decision was made to maintain a clean architecture with clear boundaries between services and avoid direct calls from one service to another.

## Consequences

- Positive: Improved scalability due to microservices architecture
- Positive: Easier debugging of code
- Negative: Minor configuration change required
## Alternatives Considered

- Alternative 1: Retain monolithic 'file-service' and avoid any additional configuration changes
- Alternative 2: Consider using an external load balancer to handle gateway's request routing

## Technical Details

- **Commit Hash:** `a280fc0fbf15fdd197b0e3ba5ecfb8500c6ffaa4`
- **Files Changed:** 1
- **Lines Added:** 10
- **Lines Removed:** 0
- **Affected Areas:** service, infrastructure

## Related Files

- `backend/gateway/src/main/resources/application.yml` (+0/-0)

