package com.bbmovie.ai_assistant_service.handler.processor;

import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.dto.ToolExecutionContext;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.service.facade.ToolWorkflow;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolResponseProcessor implements ResponseProcessor {

    @NonNull private final UUID sessionId;
    @NonNull private final AiMode aiMode;
    @NonNull private final ChatMemory chatMemory;
    private final ToolsRegistry toolRegistry;
    @NonNull private final SystemMessage systemPrompt;
    @NonNull private final ToolWorkflow toolWorkflow;
    @NonNull private final FluxSink<ChatStreamChunk> sink;
    private final long requestStartTime;

    @Override
    public Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        ToolExecutionContext context = ToolExecutionContext.builder()
                .sessionId(sessionId)
                .aiMode(aiMode)
                .aiMessage(aiMessage)
                .chatMemory(chatMemory)
                .toolRegistry(toolRegistry)
                .systemPrompt(systemPrompt)
                .sink(sink)
                .requestStartTime(requestStartTime)
                .responseMetadata(metadata)
                .build();

        return toolWorkflow.execute(context);
    }
}
