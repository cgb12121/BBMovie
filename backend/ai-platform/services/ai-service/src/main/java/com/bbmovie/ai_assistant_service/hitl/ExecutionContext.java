package com.bbmovie.ai_assistant_service.hitl;

import com.bbmovie.ai_assistant_service.exception.RequiresApprovalException;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExecutionContext {
    private String userId;
    private UUID sessionId;
    private String messageId;
    private String internalApprovalToken; // The secret token passed during retry
    private RequiresApprovalException pendingException; // Signals that approval is required
}
