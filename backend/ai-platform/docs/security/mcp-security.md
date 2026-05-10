# MCP & Agentic AI Security

Implementing a distributed AI architecture using the Model Context Protocol (MCP) introduces unique security challenges. Because the LLM orchestration (`agentic-ai`) and the actual tool execution (`mcp-server`) run in separate microservices, user authentication and authorization must seamlessly span the network boundary.

This document outlines the strategy for securing the agentic platform using **Spring AI MCP Security Extensions**.

## The Security Challenge

1. A user connects to `agentic-ai` with a valid JWT.
2. The user asks the AI to perform a sensitive action (e.g., "Ban user X").
3. The LLM decides to use the `manageUserAccount` tool, which lives on the remote `mcp-server`.
4. **The Problem:** How does `mcp-server` know if the user talking to `agentic-ai` has the `ROLE_ADMIN` authority to execute this tool?

## Solution: Spring MCP Security

We utilize the `mcp-client-security-spring-boot` and `mcp-server-security-spring-boot` libraries provided by the Spring AI community. These extensions automatically handle the propagation of the Spring Security `Authentication` context over the MCP protocol.

### 1. The Client Side (`agentic-ai`)

When a user initiates a chat session, `agentic-ai` validates their JWT using standard Spring Security mechanisms. When the LLM decides to call a remote tool, the MCP Client Security extension intercepts the outgoing MCP request and automatically attaches the current user's security context (e.g., their JWT or roles) to the MCP metadata/headers.

### 2. The Server Side (`mcp-server`)

When the `mcp-server` receives an SSE tool execution request, the MCP Server Security extension intercepts the incoming request, extracts the security context from the metadata, and reconstructs the Spring Security `Authentication` object locally.

### 3. Tool-Level Authorization (`@PreAuthorize`)

Because the `Authentication` object is successfully reconstructed on the server, we can rely on standard Spring Security method-level authorization.

Developers must secure every sensitive tool in the `mcp-server` using the `@PreAuthorize` annotation:

```java
@Bean
@Description("Manages a user account, allowing actions like banning, unbanning, or changing roles.")
@PreAuthorize("hasRole('ADMIN')") // This evaluates perfectly using the propagated context!
public Function<ManageUserRequest, ManageUserResponse> manageUserAccount() {
    return request -> {
        // Safe to assume the user is an admin here
        return new ManageUserResponse("Success");
    };
}
```

If the original user lacks the `ADMIN` role, the `mcp-server` throws an `AccessDeniedException`. This exception is caught by the MCP framework, serialized, and returned to `agentic-ai`. `agentic-ai` then informs the LLM that the tool execution failed due to insufficient permissions.

## Best Practices

1. **Defense in Depth**: Do not solely rely on the LLM's system prompt to prevent unauthorized actions. While prompting (`"You are talking to a standard user, do not use admin tools"`) helps prevent hallucinated tool calls and wasted tokens, the ultimate source of truth *must* be the `@PreAuthorize` annotation on the tool itself.
2. **Granular Roles**: Define specific roles for specific tools (e.g., `hasAuthority('TOOL_PROCESS_DOCUMENT')`) rather than relying on broad roles like `USER` or `ADMIN`.
3. **Audit Logging**: Ensure the `mcp-server` logs the identity of the user executing the tool (which is available in the `SecurityContextHolder`), not just the identity of the `agentic-ai` service account.
