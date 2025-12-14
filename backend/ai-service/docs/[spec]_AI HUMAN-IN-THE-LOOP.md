# PRODUCT SPECIFICATION: AI HUMAN-IN-THE-LOOP (HITL)
**Project:** BBMovie Microservices
**Module:** AI Service / Security
**Version:** 3.0 (Implemented)
**Status:** Implemented
**Author:** Minato (Architect)

---

## 1. OVERVIEW

### 1.1. Context
The HITL system is designed to intercept high-risk AI tool executions (e.g., deleting data, viewing sensitive configs) and require explicit human approval before proceeding.

### 1.2. Objectives
1.  **Secure Interception:** Use AOP to intercept tool calls without polluting business logic.
2.  **Reactive Integration:** Ensure the approval flow works seamlessly with the existing `Flux<ChatStreamChunk>` architecture.
3.  **Strict Binding:** Bind approval tokens to specific Users, Sessions, and Messages to prevent replay attacks.

---

## 2. TECHNICAL ARCHITECTURE

### 2.1. AOP & Reflection Strategy
Instead of manual checks, we use Spring AOP to wrap tool methods annotated with `@RequiresApproval`.

*   **Annotation:** `@RequiresApproval(action, baseRisk)` marks methods.
*   **Aspect:** `ApprovalAspect` intercepts execution.
*   **Risk Evaluator:** `RiskEvaluator` uses Reflection to scan method arguments for `@SensitiveData` fields (e.g., Email, PII) to dynamically elevate risk.

### 2.2. Exception-Based Control Flow (Bypassing LangChain4j)
To prevent the LLM framework (LangChain4j) from swallowing exceptions or treating denials as text output:

1.  **Aspect:** Sets a `pendingException` in `ExecutionContext` and returns a dummy string ("Approval Required").
2.  **Service:** `ToolExecutionServiceImpl` checks `ExecutionContext` immediately after tool invocation. If `pendingException` is found, it is **thrown**.
3.  **Handler:** `ToolResponseHandler` catches `RequiresApprovalException`, suppresses the default error, and emits a special `approval_required` UI event.

### 2.3. Circular Dependency Resolution
*   `ChatService` depends on `ToolExecutionService` (to run tools).
*   `ToolExecutionService` depends on `ApprovalService` (to create requests).
*   `ApprovalService` depends on `ChatService` (to resume chat after approval).
*   **Solution:** `ApprovalService` uses `ObjectProvider<ChatService>` to lazily inject the chat service, breaking the cycle.

---

## 3. DATA & CONTEXT FLOW

### 3.1. Context Propagation
To ensure the `messageId` (User Message ID) is available for binding:

1.  **ChatContext:** Populated in `BaseAssistant` after saving the user message (`savedMessage.getId()`).
2.  **Factory:** Passed to `ChatResponseHandlerFactory`.
3.  **Processor:** Injected into `ToolResponseProcessor`.
4.  **Workflow:** Passed to `ToolWorkflow` -> `ExecutionContext`.
5.  **Aspect:** `ApprovalAspect` reads `messageId` from `ExecutionContext` via `ThreadLocal`.

### 3.2. Approval Request Entity
Stored in `approval_requests` table via `ApprovalRequestRepository` (using custom `DatabaseClient` implementation for flexibility).

*   **Internal Token:** A secret random UUID (not the public Request ID).
*   **Bindings:** `user_id`, `session_id`, `message_id`.
*   **Timestamps:** `created_at`, `expires_at`, `approved_at`.

---

## 4. WORKFLOWS

### 4.1. Blocking Flow (User triggers action)
1.  User: "Delete user 123"
2.  AI: Calls `deleteUser(123)`
3.  **Aspect:** Detects High Risk. No Token.
4.  **Aspect:** Creates DB Request. Sets `pendingException`. Returns "Approval Required".
5.  **Service:** Throws `RequiresApprovalException`.
6.  **Handler:** Emits `ChatStreamChunk` with `permissionRequired=true`.
7.  **Frontend:** Shows Approval Modal.

### 4.2. Approval Flow (User accepts)
1.  User clicks "Approve".
2.  **API:** `POST /approve/{requestId}` with `{decision: "APPROVE"}`.
3.  **Controller:** Delegates to `ApprovalService.handleDecision`.
4.  **Service:**
    *   Validates Request ID & User.
    *   Updates status to `APPROVED` and sets `approved_at`.
    *   Retrieves **Internal Token**.
    *   Constructs `ChatRequestDto` with `internalApprovalToken`.
    *   Calls `ChatService.chat()` with `AiMode.THINKING` and correct `AssistantType`.
5.  **Retry:** AI executes tool again. Aspect sees Token. Allows execution.

### 4.3. Rejection Flow (User rejects)
1.  User clicks "Reject".
2.  **API:** `POST /approve/{requestId}` with `{decision: "REJECT"}`.
3.  **Service:**
    *   Updates status to `REJECTED`.
    *   Calls `ChatService` with system message: "User rejected...".
4.  **AI:** Acknowledges cancellation.

---

## 5. API SPECIFICATION

### 5.1. Response Chunk (SSE)
```json
{
  "type": "approval_required",
  "permissionRequired": true,
  "content": "Action requires approval.",
  "approvalRequest": {
    "requestId": "uuid",
    "actionType": "DELETE_RESOURCE",
    "riskLevel": "HIGH",
    "description": "Delete User Account"
  }
}
```

### 5.2. Approval Endpoint
*   **URL:** `/api/v1/chat/{sessionId}/approve/{requestId}`
*   **Method:** `POST`
*   **Body:**
    ```json
    { "decision": "APPROVE" } // or "REJECT"
    ```