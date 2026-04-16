# 41. Refactor(ai-service): restructure package hierarchy, add `AiModeConfiguration`, 

**Status:** Proposed  
**Date:** 2025-10-28 (Decision window: 3 days)  
**Deciders:** Cao Gia Bao  
**Commit:** bd817c34 (17 commits in window)

## Context

The BBMovie platform has decomposed the monolithic file-service into specialized microservices to improve development velocity and code maintainability, address security compliance requirements, optimize costs, and enhance resource efficiency. The file-service was replaced by three new services: media-upload-service for upload presign URLs and file management, media-service for metadata management and source of truth, and media-streaming-service for streaming files. Additionally, the transcode-worker service in Rust handles CPU-intensive tasks such as video transcoding.

**Commit Message:** refactor(ai-service): restructure package hierarchy, add `AiModeConfiguration`, and restructure imports for configurable AI modes




**Decision Window:** This ADR represents 17 related commits over 3 days:
- 136c7a17: refactor(ai-service): replace JwtUtils with _PromptLoader, r
- 9fbc6224: refactor(auth-service): implement multi-algorithm password e
- a2dc00ae: refactor(auth-service): reorganize service structure, migrat
- d621183a: refactor(ai-service): implement experimental database suppor
- c0977c3c: refactor(ai-service): enhance chat history persistence logic
... and 12 more commits

## Decision

The BBMovie platform decided to decompose its monolithic 'file-service' into the microservices: media-upload-service (for upload presign URLs and file management), media-service (for metadata management and source of truth), and media-streaming-service (for streaming files). This was done to improve development velocity, address security compliance requirements, optimize costs, and enhance resource efficiency.

## Consequences

- Positive: Improved development velocity and maintainability
- Positive: Addressed security compliance requirements
- Negative: Trade-off between performance and simplicity
- Negative: Risk of over-reliance on NATS JetStream for file operations
## Alternatives Considered

- Alternative 1: Retaining the monolithic 'file-service' to maintain a simple API interface and reduce complexity. However, this would have resulted in slower development velocity and increased costs.
- Alternative 2: Decomposing the file-service into multiple microservices with shared infrastructure, which would have led to higher resource usage and increased risk of over-reliance on NATS JetStream.

## Technical Details

- **Commit Hash:** `bd817c34377ea431ea03b80b2e5f0f2d2eb2b97d`
- **Files Changed:** 10
- **Lines Added:** 3595
- **Lines Removed:** 0
- **Affected Areas:** service, security, refactor, infrastructure, database, messaging, technology

## Related Files

- `...mingUserAssistant.java => _AdminAssistant.java}` (+0/-0)
- `.../{ToolExperimental.java => _AdminTool.java}` (+0/-0)
- `.../_low_level/{AiTool.java => _AiTool.java}` (+0/-0)
- `...imentalChatListener.java => _ChatListener.java}` (+0/-0)
- `...ntroller.java => _StreamingChatController.java}` (+0/-0)
- `.../com/bbmovie/auth/security/SecurityConfig.java` (+0/-0)
- `.../bbmovie/auth/service/auth/AuthServiceImpl.java` (+0/-0)
- `backend/auth/src/main/resources/application.yml` (+0/-0)
- `.../bbmovie/auth/controller/AuthController.java` (+0/-0)
- `.../bbmovie/auth/controller/SessionController.java` (+0/-0)

