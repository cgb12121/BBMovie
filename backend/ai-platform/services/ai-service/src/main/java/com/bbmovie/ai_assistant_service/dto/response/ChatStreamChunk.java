package com.bbmovie.ai_assistant_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChatStreamChunk {

    private String type; // "assistant", "user", "tool", "system"
    private String content; // partial or complete text

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String thinking; // AI's thinking process

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> metadata = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RagMovieDto> ragResults; // Optional — if RAG used

    /**
     * Indicates if the action requires user permission/approval.
     * Frontend should check this flag to render the approval UI.
     */
    @Builder.Default
    private boolean permissionRequired = false;

    /**
     * Payload for HITL. Present ONLY when permissionRequired=true.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ApprovalInfo approvalRequest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalInfo {
        private String requestId;      // UUID exposed to a client
        private String actionType;     // e.g., "DELETE_DATA"
        private String riskLevel;      // "MEDIUM", "HIGH", "EXTREME"
        private String description;    // "Delete user profile for id: 123"
        private Map<String, String> displayParams; // Key-Value pairs for UI display
    }

    public static ChatStreamChunk assistant(String content) {
        return ChatStreamChunk.builder()
                .type("assistant")
                .content(content)
                .build();
    }

    public static ChatStreamChunk system(String content) {
        return ChatStreamChunk.builder()
                .type("system")
                .content(content)
                .build();
    }

    public static ChatStreamChunk ragResult(List<RagMovieDto> movies) {
        return ChatStreamChunk.builder()
                .type("rag_result")
                .ragResults(movies)
                .build();
    }

    public static ChatStreamChunk approvalRequired(String requestId, String actionType, String riskLevel, String description) {
        return ChatStreamChunk.builder()
                .type("approval_required")
                .permissionRequired(true)
                .content("Action requires approval.")
                .approvalRequest(ApprovalInfo.builder()
                        .requestId(requestId)
                        .actionType(actionType)
                        .riskLevel(riskLevel)
                        .description(description)
                        .build())
                .build();
    }
}