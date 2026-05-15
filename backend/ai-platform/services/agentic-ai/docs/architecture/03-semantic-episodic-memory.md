# Advanced Memory: Semantic Episodic Recall

## The Problem
Short-term memory (`HybridChatMemory`) is limited to a "window" (e.g., the last 20 messages). If a user refers to something said 50 messages ago, the AI will have "amnesia" regarding that specific detail, even if the user identity is preserved via `PersonalizationService`.

## Proposed Solution: Vector-based Episodic Memory
Instead of just relying on the recent context window, index every conversation into the Vector Database for semantic retrieval.

### Architectural Flow:
1.  **Indexing (Asynchronous):**
    *   The `MemoryWorker`, after saving a message to SQL, also creates a `Document` for the Vector DB.
    *   **Metadata:** `{ "userId": "...", "sessionId": "...", "type": "CONVERSATION", "timestamp": "..." }`
    *   **Content:** A summarized or raw version of the user/AI exchange.
2.  **Retrieval (Intelligence Phase):**
    *   In the `PersonalizationService`, before building the system prompt, perform a two-stage search:
        *   **Stage 1:** Retrieve current User Persona (Preferences/Behaviors).
        *   **Stage 2:** Perform a semantic search on the Vector DB filtered by `userId` and `type: CONVERSATION` using the *current user query* as the search string.
3.  **Synthesis:**
    *   Inject the top 3-5 most relevant "memories" into the system prompt under a section: `RELEVANT PAST CONVERSATIONS`.

## How to Determine Long History Retrieval
To avoid cluttering the prompt with irrelevant old data:
*   **Thresholding:** Only include memories with a high similarity score (> 0.8).
*   **Summarization:** Use a small, fast model to summarize retrieved past exchanges into bullet points before injecting them into the main prompt.
*   **Recency Bias:** Optionally weight recent semantic matches higher than very old ones.

## Benefits
*   **True Long-term Memory:** The agent feels "human" by remembering details from weeks ago.
*   **Context Efficiency:** You don't need a huge context window; you only pull in the *relevant* parts of history.
