# Agentic AI Service

The `agentic-ai` service acts as the core orchestration layer for the AI platform. It utilizes **Spring AI** to coordinate interactions between the user, the Large Language Model (LLM), and various tools (both local and remote).

## Architecture Overview

This service replaces the legacy LangChain4j monolith. Instead of tightly coupling all tools into a single service, `agentic-ai` acts as an **MCP Client** (Model Context Protocol). It executes basic, low-risk tools locally and delegates advanced or sensitive tools to external MCP servers (such as `mcp-server`).

### Key Components

*   **ChatController (`bbmovie.ai_platform.agentic_ai.controller.ChatController`)**: Exposes the REST API for streaming chat responses using Server-Sent Events (SSE) via Spring WebFlux.
*   **ChatService (`bbmovie.ai_platform.agentic_ai.service.ChatService`)**: Uses Spring AI's `ChatClient` to generate responses. It intelligently decides when to fulfill a user request using an LLM or when to invoke an integrated tool.
*   **BasicTools (`bbmovie.ai_platform.agentic_ai.tool.BasicTools`)**: Contains simple tools that are safe to run within the orchestration layer (e.g., fetching system configurations).
*   **Aspect-Oriented Programming (AOP)**:
    *   **MetricsAspect**: Captures execution times and performance metrics for chat services and tool invocations.
    *   **ApprovalAspect**: Provides the foundation for the Human-In-The-Loop (HITL) system, evaluating risk levels before executing critical tool functions.

## Connection to MCP Server

The service acts as an MCP client and connects to remote tools using Server-Sent Events (SSE). This is configured in `application.properties`:

```properties
spring.ai.mcp.client.name=agentic-ai-client
spring.ai.mcp.client.type=sse
spring.ai.mcp.client.sse.connections.mcp-server.url=http://localhost:8081/mcp/sse
```
