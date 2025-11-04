package com.bbmovie.ai_assistant_service.core.low_level._utils;

import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.lang.Nullable;

import java.util.List;
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

    public static _Metrics getChatMetrics(
            long latency, ChatResponseMetadata metadata, List<ToolExecutionRequest> toolRequests) {
        Integer promptTokens = Optional.ofNullable(metadata)
                .map(ChatResponseMetadata::tokenUsage)
                .map(TokenUsage::inputTokenCount)
                .orElse(0);
        Integer responseTokens = Optional.ofNullable(metadata)
                .map(ChatResponseMetadata::tokenUsage)
                .map(TokenUsage::outputTokenCount)
                .orElse(0);
        String modelName = Optional.ofNullable(metadata)
                .map(ChatResponseMetadata::modelName)
                .orElse("unknown");

        String tools = String.join(", ",
                toolRequests.stream()
                        .map(ToolExecutionRequest::name)
                        .toList()
        );

        return _Metrics.builder()
                .latencyMs(latency)
                .promptTokens(promptTokens)
                .responseTokens(responseTokens)
                .modelName(modelName)
                .tool(tools)
                .build();
    }
}
