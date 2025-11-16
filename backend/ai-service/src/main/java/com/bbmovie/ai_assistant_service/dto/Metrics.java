package com.bbmovie.ai_assistant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    private Long latencyMs;
    private Integer promptTokens;
    private Integer responseTokens;
    private String modelName;
    private String tool;  // Tool name if this is a tool execution
}
