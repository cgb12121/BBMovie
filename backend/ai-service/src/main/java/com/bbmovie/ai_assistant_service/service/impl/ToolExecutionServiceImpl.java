package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.config.ai.ModelSelector;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.exception.RequiresApprovalException;
import com.bbmovie.ai_assistant_service.hitl.ApprovalContextHolder;
import com.bbmovie.ai_assistant_service.hitl.ExecutionContext;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.ToolExecutionService;
import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolExecutionServiceImpl implements ToolExecutionService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ToolExecutionServiceImpl.class);

    private final AuditService auditService;
    private final ModelSelector modelSelector;

    @Override
    public Mono<ToolExecutionResultMessage> execute(
            UUID sessionId, 
            ToolExecutionRequest request, 
            ToolsRegistry toolRegistry, 
            ChatMemory chatMemory,
            ExecutionContext executionContext) {

        ToolExecutor toolExecutor = toolRegistry.getExecutor(request.name());

        if (toolExecutor == null) {
            log.debug("[tool] Could not find executor for tool={}", request.name());
            String error = "Tool '" + request.name() + "' not found.";

            AuditRecord auditRecord = AuditRecord.builder()
                    .sessionId(sessionId)
                    .type(InteractionType.ERROR)
                    .details(error)
                    .metrics(null)
                    .build();
            return auditService.recordInteraction(auditRecord)
                    .thenReturn(ToolExecutionResultMessage.from(request, error))
                    .doOnNext(chatMemory::add);
        }

        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
                    // Set context for AOP
                    if (executionContext != null) {
                        ApprovalContextHolder.set(executionContext);
                        log.debug("[HITL_DEBUG] Service Set Context {}", System.identityHashCode(executionContext));
                    }
                    try {
                        String executionResult = toolExecutor.execute(request, sessionId);

                        // HITL Check: Did the aspect request approval?
                        if (executionContext != null) {
                            log.debug("[HITL_DEBUG] Service Check Pending: {} on Context {}", executionContext.getPendingException(), System.identityHashCode(executionContext));
                            
                            if (executionContext.getPendingException() != null) {
                                throw executionContext.getPendingException();
                            }
                        }

                        long latency = System.currentTimeMillis() - start;

                        Metrics metrics = MetricsUtil.get(latency, null,
                                modelSelector.getModelName(), request.name());

                        log.debug("[tool] Tool '{}' executed in {}ms: {}", request.name(), latency, executionResult);

                        return new ToolExecutionResult(
                                ToolExecutionResultMessage.from(request, executionResult),
                                metrics,
                                null
                        );
                    } catch (Exception exception) {
                        // Let AOP exception propagate if it is RequiresApprovalException?
                        // If it's RequiresApprovalException, it's a RuntimeException, so it's caught here.
                        // We must re-throw it so that downstream can handle it (create chunk).
                        
                        // BUT, wait. This `executes` returns Mono<ToolExecutionResultMessage>.
                        // If we throw here, the Mono fails.
                        // We should probably check if it is RequiresApprovalException and rethrow.
                        if (exception instanceof RequiresApprovalException) {
                            throw exception;
                        }
                        
                        // Other exceptions -> Log and return error result
                        long latency = System.currentTimeMillis() - start;
                        log.error("[tool] Error executing tool '{}': {}", request.name(), exception.getMessage(), exception);

                        Metrics metrics = MetricsUtil.get(latency, null,
                                modelSelector.getModelName(), request.name());

                        String errorMsg = "Error: " + exception.getMessage();
                        return new ToolExecutionResult(
                                ToolExecutionResultMessage.from(request, errorMsg),
                                metrics,
                                exception
                        );
                    } finally {
                        ApprovalContextHolder.clear();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic()) // Tool execution might be blocking
                .flatMap(result -> {
                    // Record audit based on success/failure
                    return recordToolExecutionAudit(sessionId, request, result);
                })
                .doOnNext(chatMemory::add);
    }

    private Mono<ToolExecutionResultMessage> recordToolExecutionAudit(
            UUID sessionId, ToolExecutionRequest request, ToolExecutionResult result) {
        InteractionType type = result.error != null
                ? InteractionType.ERROR
                : InteractionType.TOOL_EXECUTION_RESULT;

        Object details = result.error != null
                ? result.error.getMessage()
                : request;

        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(sessionId)
                .type(type)
                .details(details)
                .metrics(result.metrics)
                .build();
        return auditService.recordInteraction(auditRecord)
                .thenReturn(result.message);
    }

    private record ToolExecutionResult(ToolExecutionResultMessage message, Metrics metrics, Exception error) { }
}
