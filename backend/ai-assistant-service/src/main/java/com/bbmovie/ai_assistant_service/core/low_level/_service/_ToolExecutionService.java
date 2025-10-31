package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class _ToolExecutionService {

    private final _ChatMessageService messageService;

    public Mono<ToolExecutionResultMessage> executeAndSave(String sessionId, ToolExecutionRequest request, _ToolRegistry toolRegistry, ChatMemory chatMemory) {
        log.info("[tool] Model requested tool={} args={}", request.name(), request.arguments());
        ToolExecutor toolExecutor = toolRegistry.getExecutor(request.name());

        Mono<ToolExecutionResultMessage> toolResultMono;

        if (toolExecutor == null) {
            log.error("[tool] Could not find executor for tool={}", request.name());
            String error = "Tool '" + request.name() + "' not found.";
            toolResultMono = messageService.saveToolResult(sessionId, error)
                    .thenReturn(ToolExecutionResultMessage.from(request, error));
        } else {
            try {
                String executionResult = toolExecutor.execute(request, sessionId);
                log.info("[tool] Tool '{}' executed: {}", request.name(), executionResult);
                toolResultMono = messageService.saveToolResult(sessionId, executionResult)
                        .thenReturn(ToolExecutionResultMessage.from(request, executionResult));
            } catch (Exception e) {
                log.error("[tool] Error executing tool '{}': {}", request.name(), e.getMessage(), e);
                String error = "Error executing tool '" + request.name() + "': " + e.getMessage();
                toolResultMono = messageService.saveToolResult(sessionId, error)
                        .thenReturn(ToolExecutionResultMessage.from(request, error));
            }
        }

        return toolResultMono.doOnNext(chatMemory::add);
    }
}
