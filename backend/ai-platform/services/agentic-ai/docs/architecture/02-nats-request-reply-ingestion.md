# Reliable Ingestion: NATS Request-Reply

## The Problem
The current `ContentRoutingService` uses a `WebClient` retry loop with a backoff strategy to wait for file ingestion (parsing/embedding). This is "busy-waiting": it consumes network resources and thread time polling a service that might take several minutes to finish.

## Proposed Solution: NATS Request-Reply
Transition from HTTP polling to an event-driven "Sync-over-Async" pattern using NATS JetStream.

### The Flow:
1.  **Request:** The `agentic-ai` service detects an asset is not yet `READY`. Instead of retrying HTTP, it sends a NATS request:
    *   **Subject:** `ingestion.wait.ready.{assetId}`
    *   **ReplyTo:** A unique temporary subject created by the NATS client.
2.  **Processing:** The `ai-assets` service (or a dedicated Ingestion Coordinator) receives the request.
    *   If the asset is already done, it replies immediately.
    *   If the asset is still processing, it puts the `ReplyTo` address into a "waiting list" (or uses NATS internal request-reply timeout mechanism).
3.  **Completion:** Once the Java/Rust ingestion engine finishes, it publishes a `READY` event. The coordinator then replies to all pending NATS requests for that `{assetId}`.
4.  **Resilience:** The `agentic-ai` service receives the reply and proceeds with the chat request.

## Benefits
*   **Efficiency:** No unnecessary HTTP calls or "polling lag."
*   **Scalability:** Thousands of users can wait for different assets without overwhelming the `ai-assets` REST API.
*   **Responsiveness:** The chat starts *instantly* the second the ingestion is complete.
