package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface _ToolWorkflowFacade {
    Mono<Void> executeWorkflow(
            UUID sessionId,
            _AiMode aiMode,
            AiMessage aiMessage,
            ChatMemory chatMemory,
            _ToolsRegistry toolRegistry,
            SystemMessage systemPrompt,
            FluxSink<String> sink,
            long requestStartTime
    );
}
