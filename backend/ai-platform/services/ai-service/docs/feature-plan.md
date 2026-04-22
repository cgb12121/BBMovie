# FEATURE PLAN: AI HUMAN-IN-THE-LOOP (HITL) IMPLEMENTATION

**Version:** 1.0
**Focus:** DTOs, AOP, and Reactive Integration
**Objective:** Implement a secure, reflection-based approval system for AI tools using Spring AOP.

---

## STEP 1: DATA TRANSFER OBJECTS (DTOs) & API CONTRACT [CRITICAL]

*This step defines how the Backend communicates with the Frontend. It must be implemented first so the Client team can start working on the UI (Popup/Modal).*

### 1.1 Update `ChatStreamChunk` (Response)
Modify the existing `ChatStreamChunk` to include the `approvalRequest` field. This is how the server signals "STOP & ASK".

```java
// File: com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk.java

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChatStreamChunk {
    // ... existing fields (type, content, etc.) ...

    /**
     * Payload for HITL. Present ONLY when type="approval_required".
     */
    private ApprovalInfo approvalRequest; 

    @Data
    @Builder
    public static class ApprovalInfo {
        private String requestId;      // UUID exposed to client
        private String actionType;     // e.g., "DELETE_DATA"
        private String riskLevel;      // "MEDIUM", "HIGH", "EXTREME"
        private String description;    // "Delete user profile for id: 123"
        private Map<String, String> displayParams; // Key-Value pairs for UI display (e.g., "Email": "new@example.com")
    }
}
```

### 1.2 Create `ApprovalDecisionDto` (Request)
Define the payload for the user's decision.

```java
// File: com.bbmovie.ai_assistant_service.dto.request.ApprovalDecisionDto.java

public record ApprovalDecisionDto(
    @NotNull Decision decision
) {
    public enum Decision {
        APPROVE,
        REJECT
    }
}
```

---

## STEP 2: CORE DOMAIN & PERSISTENCE

### 2.1 Enums & Annotations
Create the metadata markers for Reflection.

*   `RiskLevel` (NONE, LOW, MEDIUM, HIGH, EXTREME)
*   `ActionType` (DELETE, UPDATE, CONFIG, etc.)
*   `@RequiresApproval(baseRisk, action)` - Method level.
*   `@SensitiveData(level)` - Field level (for DTOs).

### 2.2 `ApprovalRequest` Entity
The persistent record of the request.

*   **Key Fields:** `id` (Public), `internalToken` (Secret), `userId`, `sessionId`, `messageId`, `status`.
*   **Constraint:** `internalToken` MUST be randomly generated and NEVER sent to the client.

---

## STEP 3: AOP & REFLECTION INFRASTRUCTURE

### 3.1 `ApprovalContextHolder`
Since AOP runs on the tool method (which might not have direct access to the web request), we use a `ThreadLocal` to pass context from the `ToolExecutor` to the `Aspect`.

```java
public class ApprovalContextHolder {
    private static final ThreadLocal<ExecutionContext> CONTEXT = new ThreadLocal<>();
    // set(), get(), clear()
}
```

### 3.2 `RiskEvaluator` (Reflection Logic)
A service that:
1.  Reads `@RequiresApproval` from the method.
2.  Iterates through method arguments.
3.  Recursively checks fields annotated with `@SensitiveData`.
4.  Returns the **Highest** detected `RiskLevel`.

### 3.3 `ApprovalAspect` (The Guard)
The AOP Aspect that wraps tool executions.

*   **Pointcut:** `@annotation(RequiresApproval)`
*   **Logic:**
    1.  Get `ExecutionContext` from `ApprovalContextHolder`.
    2.  Call `RiskEvaluator` to get `currentRisk`.
    3.  If `currentRisk < MEDIUM`, `proceed()`.
    4.  If `currentRisk >= MEDIUM`:
        *   Check `context.internalToken`.
        *   If valid -> `proceed()`.
        *   If invalid -> Call `ApprovalService.createRequest()` -> Throw `RequiresApprovalException`.

---

## STEP 4: SERVICE LAYER

### 4.1 `ApprovalService`
*   `createRequest(...)`: Generates Public ID & Internal Secret. Saves to DB.
*   `validateAndGetToken(...)`: Verifies Public ID + User ID + Session ID. Returns Internal Secret.
*   `reject(...)`: Updates status to REJECTED.

### 4.2 `ChatService` Integration
*   **Exception Handling:** Catch `RequiresApprovalException`.
*   **Transformation:** Convert exception into `ChatStreamChunk` (type=`approval_required`).
*   **Context Propagation:** Ensure `ApprovalContextHolder` is set before invoking LLM/Tools.

---

## STEP 5: API ENDPOINTS

### 5.1 Update `ChatController`
Add the decision endpoint.

*   `POST /api/v1/chat/{sessionId}/approve/{requestId}`
*   **Flow (Approve):**
    1.  Call `ApprovalService` to validate.
    2.  Get `internalToken`.
    3.  Reconstruct `ChatRequestDto`.
    4.  Inject `internalToken` into DTO.
    5.  Call `ChatService.chat()`.
*   **Flow (Reject):**
    1.  Call `ApprovalService.reject()`.
    2.  Call `ChatService.chat()` with "User cancelled..." message.

---

## SUMMARY OF UI/CLIENT IMPACT

1.  **Listen** for SSE event `type: "approval_required"`.
2.  **Display** Modal using `approvalRequest` data (Risk Level, Description).
3.  **Action:**
    *   **Approve:** POST to `/approve/{id}` with `{decision: "APPROVE"}`.
    *   **Reject:** POST to `/approve/{id}` with `{decision: "REJECT"}`.
4.  **Handle Response:** The `/approve` endpoint returns a *new* stream. Replace the current chat stream with this new one (or append to it).
