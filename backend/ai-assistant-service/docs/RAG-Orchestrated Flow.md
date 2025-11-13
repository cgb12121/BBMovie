# Chat Orchestration Flow (RAG-Ready)

This document explains the orchestration pipeline of the AI chat system and how to integrate **RAG (Retrieval-Augmented Generation)** — both as a **pre-context retriever** and as a **tool**.

+ 1️⃣ User sends message
+ 2️⃣ Retrieve relevant contexts via RAG (movies + chat)
+ 3️⃣ Inject them into system prompt or user message
+ 4️⃣ Save the message + context to DB
+ 5️⃣ Proceed with chat model orchestration (tool-aware)
+ 6️⃣ Audit every major step
+ 7️⃣ Log AI thinking for audit (sanitized, never sent to clients)

## Response Stream Structure

Responses are streamed as `_ChatStreamChunk` objects with the following fields:
- **`type`**: Chunk type (`"assistant"`, `"rag_result"`, etc.)
- **`content`**: Streamed text content (token-by-token)
- **`thinking`**: AI reasoning (audit/logging only, **not sent to clients**)
- **`ragResults`**: RAG-retrieved movies (emitted last when RAG is used)

**Note:** The `thinking` field is always sanitized and logged for audit purposes but is never included in client responses.

```mermaid
flowchart TD
    U[User Message] --> C[ChatController]
    C --> S[ChatService]
    S --> A[BaseAssistant]

    A -->|Record user audit| AUD[AuditService]
    A --> MEM[ChatMemoryProvider]
    A -->|Pre-RAG retrieval| RAG1[RagService.retrieveRelevantContext]
    RAG1 --> A

    A -->|Build ChatRequest with RAG context| M[StreamingChatModel]
    M --> H[ToolExecutingResponseHandler]

    H -->|AI_COMPLETE| AUD
    H -->|Log thinking `audit only`| AUD
    H --> MSG[MessageService]
    H -->|Tool Calls?| T{ToolExecution?}

    T -->|Yes| TE[ToolExecutionService]
    TE -->|Maybe `RAG Tool`| RAG2[RagService.retrieveRelevantContext]
    TE -->|Audit| AUD
    TE --> A

    RAG2 -->|Feed RAG context| H

    T -->|No| DONE[(Stream Complete)]
    H -->|Post-RAG indexing| RAG3[RagService.indexConversationFragment]
    RAG3 --> AUD

    style U fill:#ddeeff,stroke:#4477aa
    style A fill:#fff5e1,stroke:#aa8800
    style RAG1 fill:#d9f7e9,stroke:#009944
    style RAG2 fill:#c1f0d1,stroke:#009944
    style RAG3 fill:#c1f0d1,stroke:#009944
    style M fill:#e1f7e7,stroke:#009944
    style H fill:#fce4ec,stroke:#c2185b
    style TE fill:#f8f9d1,stroke:#999933
    style AUD fill:#e8eaf6,stroke:#3f51b5

```