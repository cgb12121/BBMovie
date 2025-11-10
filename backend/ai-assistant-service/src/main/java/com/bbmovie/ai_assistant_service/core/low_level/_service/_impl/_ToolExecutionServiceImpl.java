package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
public class _ToolExecutionServiceImpl implements _ToolExecutionService {

    private final _AuditService auditService;
    private final _ModelSelector modelSelector;

    @Autowired
    public _ToolExecutionServiceImpl(_AuditServiceImpl auditService, _ModelSelector modelSelector) {
        this.auditService = auditService;
        this.modelSelector = modelSelector;
    }

    @Override
    public Mono<ToolExecutionResultMessage> execute(
            UUID sessionId, ToolExecutionRequest request, _ToolsRegistry toolRegistry, ChatMemory chatMemory) {

        ToolExecutor toolExecutor = toolRegistry.getExecutor(request.name());

        if (toolExecutor == null) {
            log.debug("[tool] Could not find executor for tool={}", request.name());
            String error = "Tool '" + request.name() + "' not found.";

            _AuditRecord auditRecord = _AuditRecord.builder()
                    .sessionId(sessionId)
                    .type(_InteractionType.ERROR)
                    .details(error)
                    .metrics(null)
                    .build();
            return auditService.recordInteraction(auditRecord)
                    .thenReturn(ToolExecutionResultMessage.from(request, error))
                    .doOnNext(chatMemory::add);
        }

        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
                    try {
                        String executionResult = toolExecutor.execute(request, sessionId);
                        long latency = System.currentTimeMillis() - start;

                        _Metrics metrics = _MetricsUtil.get(latency, null,
                                modelSelector.getModelName(), request.name());

                        log.debug("[tool] Tool '{}' executed in {}ms: {}",
                                request.name(), latency, executionResult);

                        // Create result and metrics together
                        return new ToolExecutionResult(
                                ToolExecutionResultMessage.from(request, executionResult),
                                metrics,
                                null
                        );
                    } catch (Exception exception) {
                        long latency = System.currentTimeMillis() - start;
                        log.error("[tool] Error executing tool '{}': {}", request.name(), exception.getMessage(), exception);

                        _Metrics metrics = _MetricsUtil.get(latency, null,
                                modelSelector.getModelName(), request.name());

                        String errorMsg = "Error: " + exception.getMessage();
                        return new ToolExecutionResult(
                                ToolExecutionResultMessage.from(request, errorMsg),
                                metrics,
                                exception
                        );
                    }
                })
                .subscribeOn(Schedulers.boundedElastic()) // Tool execution might be blocking
                .flatMap(result -> {
                    // Record audit based on success/failure
                    _InteractionType type = result.error != null
                            ? _InteractionType.ERROR
                            : _InteractionType.TOOL_EXECUTION_RESULT;

                    Object details = result.error != null
                            ? result.error.getMessage()
                            : request;

                    _AuditRecord auditRecord = _AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(type)
                            .details(details)
                            .metrics(result.metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(result.message);
                })
                .doOnNext(chatMemory::add);
    }

        private record ToolExecutionResult(
                ToolExecutionResultMessage message,
                _Metrics metrics,
                Exception error
        ) {
    }
}