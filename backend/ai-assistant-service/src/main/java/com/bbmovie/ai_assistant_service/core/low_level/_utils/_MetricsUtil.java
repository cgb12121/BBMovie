package com.bbmovie.ai_assistant_service.core.low_level._utils;

import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.lang.Nullable;

import java.util.Optional;

public class _MetricsUtil {

    private _MetricsUtil() {}

    public static _Metrics get(
            long latency, @Nullable TokenUsage usage, String modelName, String toolName) {
        return _Metrics.builder()
                .latencyMs(latency)
                .promptTokens(
                        Optional.ofNullable(usage)
                                .map(TokenUsage::inputTokenCount)
                                .orElse(0)
                )
                .responseTokens(
                        Optional.ofNullable(usage)
                                .map(TokenUsage::outputTokenCount)
                                .orElse(0)
                )
                .modelName(modelName)
                .tool(toolName)
                .build();
    }
}
