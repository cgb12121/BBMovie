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
import reactor.core.publisher.MonoSink;

import java.util.UUID;

public class _ToolResponseProcessor implements _ResponseProcessor {

    private final UUID sessionId;
    private final _AiMode aiMode;
    private final ChatMemory chatMemory;
    private final _ToolsRegistry toolRegistry; // Added
    private final SystemMessage systemPrompt; // Added
    private final _ToolWorkflowFacade toolWorkflowFacade;
    private final FluxSink<String> sink;
    private final MonoSink<Void> monoSink;
    private final long requestStartTime;

    private _ToolResponseProcessor(Builder builder) {
        this.sessionId = builder.sessionId;
        this.aiMode = builder.aiMode;
        this.chatMemory = builder.chatMemory;
        this.toolRegistry = builder.toolRegistry; // Added
        this.systemPrompt = builder.systemPrompt; // Added
        this.toolWorkflowFacade = builder.toolWorkflowFacade;
        this.sink = builder.sink;
        this.monoSink = builder.monoSink;
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
                monoSink,
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
        private MonoSink<Void> monoSink;
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

        public Builder monoSink(MonoSink<Void> monoSink) {
            this.monoSink = monoSink;
            return this;
        }

        public Builder requestStartTime(long requestStartTime) {
            this.requestStartTime = requestStartTime;
            return this;
        }

        public _ToolResponseProcessor build() {
            return new _ToolResponseProcessor(this);
        }
    }
}
