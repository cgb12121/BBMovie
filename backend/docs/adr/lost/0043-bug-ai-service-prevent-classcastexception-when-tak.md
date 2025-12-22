# 43. Bug(ai-service): prevent ClassCastException when taking information from jwt

**Status:** Proposed  
**Date:** 2025-10-31  
**Deciders:** Cao Gia Bao  
**Commit:** 6d1eb71a

## Context

The BBMovie platform has decomposed the monolithic 'file-service' (legacy 'god service') into three specialized microservices to address security and compliance requirements. The file operations must now go through media-upload-service to ensure secure, compliant presigned URLs.

**Commit Message:** bug(ai-service): prevent ClassCastException when taking information from jwt





## Decision

The media-streaming-service is decided to handle direct DB access between services via APIs or events while using reactive programming with WebFlux for non-blocking I/O to support the event-driven communication via NATS JetStream. The Rust services like rust-ai-context-refinery and rust-transcode-worker are used to handle CPU-intensive tasks, such as AI processing and transcoding.

## Consequences

- Positive: Improved security and compliance
- Positive: Better maintainability and scalability of the system
- Negative: Potential trade-off between performance and complexity
- Negative: Risk of increased maintenance costs due to the additional services
## Alternatives Considered

- Alternative 1: Retaining the monolithic 'file-service' architecture
- Alternative 2: Using a different messaging system like Kafka, which might have been considered but was rejected in favor of NATS JetStream for its performance and simplicity.

## Technical Details

- **Commit Hash:** `6d1eb71aa16330d99d3a6ff698833f3869fe74b6`
- **Files Changed:** 5
- **Lines Added:** 63
- **Lines Removed:** 0
- **Affected Areas:** service, messaging, security

## Related Files

- `.gitignore` (+0/-0)
- `backend/ai-assistant-service/error.md` (+0/-0)
- `.../core/low_level/_controller/_ChatController.java` (+0/-0)
- `.../_repository/_ChatMessageRepository.java` (+0/-0)
- `.../core/low_level/_service/_AuditService.java` (+0/-0)

