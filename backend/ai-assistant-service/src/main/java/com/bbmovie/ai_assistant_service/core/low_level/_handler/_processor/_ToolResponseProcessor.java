package com.bbmovie.ai_assistant_service.core.low_level._handler._processor;

import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._service._facade._ToolWorkflowFacade;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

public class _ToolResponseProcessor implements _ResponseProcessor {

    private final UUID sessionId;
    private final _AiMode aiMode;
    private final ChatMemory chatMemory;
    private final _ToolsRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final _ToolWorkflowFacade toolWorkflowFacade;
    private final FluxSink<String> sink;
    private final long requestStartTime;

    private _ToolResponseProcessor(Builder builder) {
        this.sessionId = builder.sessionId;
        this.aiMode = builder.aiMode;
        this.chatMemory = builder.chatMemory;
        this.toolRegistry = builder.toolRegistry;
        this.systemPrompt = builder.systemPrompt;
        this.toolWorkflowFacade = builder.toolWorkflowFacade;
        this.sink = builder.sink;
        this.requestStartTime = builder.requestStartTime;
    }

    @Override
    public Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        return toolWorkflowFacade.executeWorkflow(
                sessionId,
                aiMode,
                aiMessage,
                chatMemory,
                toolRegistry,
                systemPrompt,
                sink,
                requestStartTime
        );
    }

    public static class Builder {
        private UUID sessionId;
        private _AiMode aiMode;
        private ChatMemory chatMemory;
        private _ToolsRegistry toolRegistry;
        private SystemMessage systemPrompt;
        private _ToolWorkflowFacade toolWorkflowFacade;
        private FluxSink<String> sink;
        private long requestStartTime;

        public Builder sessionId(UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder aiMode(_AiMode aiMode) {
            this.aiMode = aiMode;
            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder toolRegistry(_ToolsRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }

        public Builder systemPrompt(SystemMessage systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder toolWorkflowFacade(_ToolWorkflowFacade toolWorkflowFacade) {
            this.toolWorkflowFacade = toolWorkflowFacade;
            return this;
        }

        public Builder sink(FluxSink<String> sink) {
            this.sink = sink;
            return this;
        }

        public Builder requestStartTime(long requestStartTime) {
            this.requestStartTime = requestStartTime;
            return this;
        }

        public _ToolResponseProcessor build() {
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(aiMode, "aiMode must not be null");
            Objects.requireNonNull(chatMemory, "chatMemory must not be null");
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(toolWorkflowFacade, "toolWorkflowFacade must not be null");
            Objects.requireNonNull(sink, "sink must not be null");
            return new _ToolResponseProcessor(this);
        }
    }
}
