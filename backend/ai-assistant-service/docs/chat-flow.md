# Chat Orchestration Flow (Without RAG)

This document explains the orchestration pipeline of the AI chat system and how to integrate **RAG (Retrieval-Augmented Generation)** — both as a **pre-context retriever** and as a **tool**.

---

## Response Structure

The chat system streams responses as `_ChatStreamChunk` objects with the following structure:

- **`type`**: The type of chunk (`"assistant"`, `"user"`, `"tool"`, `"system"`, `"rag_result"`)
- **`content`**: The actual text content (streamed token-by-token for assistant responses)
- **`thinking`**: AI's reasoning process (for audit/logging only, **never sent to clients**)
- **`metadata`**: Optional metadata map
- **`ragResults`**: Optional list of RAG-retrieved movies (emitted last when RAG is used)

### Thinking Field

The `thinking` field contains the AI's reasoning trace (when using models with thinking capability like Ollama's thinking models). 

**Important Notes:**
- Thinking is **always logged** for audit purposes (sanitized to remove sensitive information)
- Thinking is **never sent to clients** in the stream
- Thinking is sanitized using `_ThinkingSanitizer` to remove:
  - Session IDs (UUIDs)
  - Internal tool names and parameters
  - System implementation details

The thinking is logged at `TRACE` level with the prefix `[thinking][audit]` for debugging and compliance purposes.

---

##  High-Level Architecture

```mermaid
flowchart TD
    A[User] -->|Sends message| B[ChatController]
    B -->|Validate & forward| C[ChatService]
    C -->|Select assistant| D[BaseAssistant]
    D -->|Record interaction USER| E[AuditService]
    E --> D
    D -->|Save user message| F[MessageService]
    F --> D

    D -->|Build ChatRequest| G[StreamingChatModel]
    G -->|Stream tokens| H[ToolExecutingResponseHandler]

    H -->|Save AI response| F
    H -->|Record interaction AI_COMPLETE| E
    H -->|Log thinking | E

    H -->|Tool requests?| I{Has Tool Calls?}
    I -->|Yes| J[ToolExecutionService]
    J -->|Execute & audit| E
    J -->|Add tool results to memory| D

    I -->|No| K[Stream Complete]
    J -->|Recursive Chat| G

    style A fill:#ddeeff,stroke:#4477aa
    style D fill:#fff5e1,stroke:#aa8800
    style G fill:#e1f7e7,stroke:#009944
    style H fill:#fce4ec,stroke:#c2185b
    style J fill:#f8f9d1,stroke:#999933
    style E fill:#e8eaf6,stroke:#3f51b5
    style F fill:#ede7f6,stroke:#7b1fa2
```