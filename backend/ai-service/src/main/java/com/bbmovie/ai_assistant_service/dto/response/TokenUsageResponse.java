package com.bbmovie.ai_assistant_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUsageResponse {
    private Long totalPromptTokens;
    private Long totalResponseTokens;
    private Long totalTokens;
}
