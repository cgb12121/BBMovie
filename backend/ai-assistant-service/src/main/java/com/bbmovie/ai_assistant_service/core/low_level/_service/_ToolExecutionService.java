package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface _ToolExecutionService {
    Mono<ToolExecutionResultMessage> execute(UUID sessionId, ToolExecutionRequest request, _ToolsRegistry toolRegistry, ChatMemory chatMemory);
}
