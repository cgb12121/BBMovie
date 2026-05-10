# MCP Server Service

The `mcp-server` acts as a remote tool provider for the AI platform. It is built using **Spring AI** and implements the **Model Context Protocol (MCP)** to securely expose advanced tools to MCP clients (like `agentic-ai`).

## Architecture Overview

By decoupling advanced, domain-specific tools from the core orchestration logic, the AI platform achieves better security, scalability, and maintainability. The `mcp-server` focuses exclusively on fulfilling tool execution requests triggered by the LLM.

### Key Components

*   **AdvancedTools (`bbmovie.ai_platform.mcpserver.tool.AdvancedTools`)**: A configuration class that exposes complex backend operations as Spring AI `@Tool` functions. Examples include:
    *   `retrieveRagMovies`: Integrates with the Vector Database (Qdrant) and `RagService` to retrieve semantic movie context.
    *   `processDocument`: Handles file parsing and ingestion via the `FileProcessingService`.
    *   `manageUserAccount`: Executes sensitive administrative actions (e.g., banning/unbanning users).

## MCP Protocol Details

The server communicates using Server-Sent Events (SSE) over Spring WebFlux. It listens for incoming tool execution requests from authorized MCP clients.

Configuration in `application.properties`:
```properties
server.port=8081
spring.ai.mcp.server.type=sse
spring.ai.mcp.server.sse.message-endpoint=/mcp/message
```

## Security

Because this server hosts sensitive tools (like database modifications or user management), it strictly enforces role-based access control (RBAC). The security context (like the user's JWT or Roles) is propagated from the `agentic-ai` client using the Spring AI MCP Security extensions. 

For more details on how security is implemented across the MCP boundary, refer to the [MCP Security Documentation](../security/mcp-security.md).
