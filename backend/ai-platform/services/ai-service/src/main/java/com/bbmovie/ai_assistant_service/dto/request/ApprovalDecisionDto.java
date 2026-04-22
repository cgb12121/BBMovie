package com.bbmovie.ai_assistant_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApprovalDecisionDto(
    @NotNull Decision decision
) {
    public enum Decision {
        APPROVE,
        REJECT
    }
}
