# 42. Chat-architecture

**Status:** Proposed  
**Date:** 2025-10-31 (Decision window: 0 days)  
**Deciders:** Cao Gia Bao  
**Commit:** 0e780fa1 (3 commits in window)

## Context

The decision to decompose the monolithic file-service into specialized microservices was driven by increasing development velocity and code maintainability. As the BBMovie platform grew in complexity, maintaining a single 'god service' became challenging. The decomposed services improved resource efficiency and reduced operational costs.

**Commit Message:** Merge pull request #1 from cgb12121/refactor/chat-architecture

**Details:** Refactor: Modular Chat Architecture


**Decision Window:** This ADR represents 3 related commits over 0 days:
- 4bd41c7e: feat(chat): Initial refactor for modular chat architecture
- 9f518f28: refactor(chat): Finalize modular architecture
- 0e780fa1: Merge pull request #1 from cgb12121/refactor/chat-architectu

## Decision

The BBMovie platform decomposed the legacy file-service into three new microservices: media-upload-service for upload presign URLs and file management, media-service for metadata management and source of truth, and media-streaming-service for streaming files to clients. This modular architecture enhances scalability and maintainability while reducing operational costs.

## Consequences

- Positive: Enhanced development velocity and code maintainability
- Positive: Reduced operational cost by optimizing resource efficiency
- Negative: Potential trade-off - increased complexity in managing multiple services
- Negative: Risk of long-term maintenance challenges due to the increasing number of microservices
## Alternatives Considered

- Alternative 1: Retaining a single 'god service' for file management, which would have led to code maintainability issues as the platform grew.
- Alternative 2: Decomposing into more services than initially planned, but this would have increased complexity and maintenance challenges.

## Technical Details

- **Commit Hash:** `0e780fa1b6a0323eb6ce1bb17af4563592ff2dc1`
- **Files Changed:** 8
- **Lines Added:** 2430
- **Lines Removed:** 0
- **Affected Areas:** service, refactor, architecture

## Related Files

- `.../core/low_level/_SessionNotFoundException.java` (+0/-0)
- `.../core/low_level/_assistant/_AdminAssistant.java` (+0/-0)
- `.../core/low_level/_config/_ToolConfig.java` (+0/-0)
- `...ingChatController.java => _ChatController.java}` (+0/-0)
- `.../low_level/_controller/_SessionController.java` (+0/-0)
- `.../core/low_level/_assistant/_Assistant.java` (+0/-0)
- `.../core/low_level/_assistant/_BaseAssistant.java` (+0/-0)
- `.../low_level/_config/_HandlerFactoryConfig.java` (+0/-0)

