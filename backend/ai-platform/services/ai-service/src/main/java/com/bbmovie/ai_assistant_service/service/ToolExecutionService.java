package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.hitl.ExecutionContext;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ToolExecutionService {
    Mono<ToolExecutionResultMessage> execute(UUID sessionId, ToolExecutionRequest request, ToolsRegistry toolRegistry, ChatMemory chatMemory, ExecutionContext executionContext);
}