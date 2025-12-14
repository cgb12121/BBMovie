package com.bbmovie.ai_assistant_service.exception;

import com.bbmovie.ai_assistant_service.hitl.ActionType;
import com.bbmovie.ai_assistant_service.hitl.RiskLevel;
import lombok.Getter;

/**
 * Thrown when a tool execution is blocked.
 * The Stream Handler catches this and emits an approval chunk.
 */
@Getter
public class RequiresApprovalException extends RuntimeException {
    private final String requestId;
    private final ActionType actionType;
    private final RiskLevel riskLevel;
    private final String description;

    public RequiresApprovalException(String requestId, ActionType actionType, RiskLevel riskLevel, String description) {
        super("Approval required: " + requestId);
        this.requestId = requestId;
        this.actionType = actionType;
        this.riskLevel = riskLevel;
        this.description = description;
    }
}
