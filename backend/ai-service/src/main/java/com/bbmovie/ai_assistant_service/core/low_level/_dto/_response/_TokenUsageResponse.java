package com.bbmovie.ai_assistant_service.core.low_level._dto._response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class _TokenUsageResponse {
    private Long totalPromptTokens;
    private Long totalResponseTokens;
    private Long totalTokens;
}
