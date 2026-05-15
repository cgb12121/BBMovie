# Observability: Token & Cost Tracking

## The Problem
Currently, token usage extraction is embedded within the `ThinkingAdvisor`. This violates the Single Responsibility Principle and makes it difficult to extend tracking logic (e.g., adding pricing calculations, per-user quotas, or exporting metrics to Prometheus/Grafana) without risking the core reasoning pipeline.

## Proposed Solution: `TokenUsageAdvisor`
Create a dedicated `TokenUsageAdvisor` that sits at the end of the advisor chain.

### Key Responsibilities:
1.  **Extraction:** Intercept the final `ChatClientResponse` and extract `Usage` metadata (prompt tokens, completion tokens, total tokens).
2.  **Event Dispatching:** Instead of saving directly to a DB (which is slow), publish a NATS event: `ai.usage.recorded`.
    ```json
    {
      "userId": "UUID",
      "sessionId": "UUID",
      "model": "gpt-4o",
      "promptTokens": 150,
      "completionTokens": 45,
      "timestamp": "ISO-8601"
    }
    ```
3.  **Decoupling:** A separate background worker (`UsageWorker`) listens to the NATS subject and persists the data to a timeseries-friendly table or a simple analytics table in SQL.

## Benefits
*   **Performance:** The user doesn't wait for usage persistence; it happens asynchronously via NATS.
*   **Maintainability:** Cost logic, quotas, and pricing are handled in a single place.
*   **Flexibility:** Different models have different costs; the `UsageWorker` can apply pricing logic based on the `model` field in the event.
