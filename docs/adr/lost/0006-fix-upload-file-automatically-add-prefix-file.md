# 6. Fix upload file automatically add prefix file:///

**Status:** Proposed  
**Date:** 2025-07-11  
**Deciders:** Cao Gia Bao  
**Commit:** e835c8ca

## Context

The decision to decompose the monolithic file-service into three specialized microservices: media-upload-service for upload presign URLs and file handling, media-service as metadata management and source of truth, and media-streaming-service for streaming files, was driven by development velocity and code maintainability. The BBMovie platform's goal is to increase development speed while ensuring a maintainable architecture.

**Commit Message:** fix upload file automatically add prefix file:///





## Decision

The BBMovie platform decided to decompose the monolithic file-service into three specialized microservices: media-upload-service for upload presign URLs and file handling, media-service as metadata management and source of truth, and media-streaming-service for streaming files. This decision aims to increase development velocity while ensuring code maintainability.

## Consequences

- Positive: Faster development velocity
- Positive: Easier code maintenance
- Negative: Potential trade-off between performance and simplicity due to the complexity of managing multiple presign URLs and file handling in media-upload-service
- Negative: Risk of introducing new bugs during the transition from monolithic to microservices architecture
## Alternatives Considered

- Alternative 1: Keeping the file-service as a monolith, which would have led to slower development velocity and more complex code maintenance.
- Alternative 2: Decomposing the file-service into multiple services but retaining the original structure of upload, metadata management, and streaming. However, this decision would have resulted in a less maintainable architecture.

## Technical Details

- **Commit Hash:** `e835c8caed7d640cbd013e18bf011b5d7447a8f8`
- **Files Changed:** 5
- **Lines Added:** 264
- **Lines Removed:** 0
- **Affected Areas:** General

## Related Files

- `...Strategy.java => FileLocalStorageStrategy.java}` (+0/-0)
- `.../service/FileUploadService.java` (+0/-0)
- `.../service/LocalFileStorageStrategy.java` (+0/-0)
- `.../service/ffmpeg/CodecOptions.java` (+0/-0)
- `.../service/ffmpeg/PresetOptions.java` (+0/-0)

