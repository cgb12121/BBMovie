# 9. Architecture Decision

**Status:** Proposed  
**Date:** 2025-07-15  
**Deciders:** Cao Gia Bao  
**Commit:** b00b3b53

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices: 1. **media-service** - Metadata management and source of truth 2. **media-upload-service** - Upload presign URLs and file handling 3. **transcode-worker** - CPU-intensive tasks such as transcoding to support different video resolutions and formats. The decision was made for cost optimization and resource efficiency to improve the platform's scalability, maintainability, and resilience.

**Commit Message:** Merge branch 'main' of https://github.com/cgb12121/BBMovie





## Decision

The media-upload-service has been migrated from the decomposed file-service to handle file operations. This migration aims to decouple file handling from other services, improving their performance and reducing dependencies on monolithic architecture.

## Consequences

- Positive: The migration ensures scalability and maintainability of the BBMovie platform.
- Positive: It reduces the risk of a single point of failure by spreading out responsibilities among microservices.
- Negative: Trade-off - Some functionality may be temporarily unavailable during the migration process.
- Negative: Risk - Incomplete testing or improper implementation could lead to performance issues or data integrity problems.
## Alternatives Considered

- Alternative 1: Retaining the monolithic architecture, which would have resulted in increased complexity and slower response times. This choice was discarded because it would not meet the platform's scalability requirements.
- Alternative 2: Keeping file operations within media-upload-service instead of migrating to a separate microservice. However, this decision was rejected due to potential performance bottlenecks and reduced flexibility.

## Technical Details

- **Commit Hash:** `b00b3b53e51486642f1297185593e8e4fb4c8c33`
- **Files Changed:** 1
- **Lines Added:** 313
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `frontend/README-FILE-SERVICE.md` (+0/-0)

