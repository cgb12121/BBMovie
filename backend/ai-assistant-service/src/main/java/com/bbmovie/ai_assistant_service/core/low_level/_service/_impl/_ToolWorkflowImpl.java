package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ToolResponseHandler;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolWorkflowFacade;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class _ToolWorkflowImpl implements _ToolWorkflowFacade {

    private static final _Logger log = _LoggerFactory.getLogger(_ToolWorkflowImpl.class);

    private final _ToolExecutionService toolExecutionService;
    private final _ModelFactory modelFactory;
    private final _AuditService auditService;
    private final _MessageService messageService;

    @Autowired
    public _ToolWorkflowImpl(
            _ToolExecutionService toolExecutionService, _ModelFactory modelFactory,
            _AuditService auditService, _MessageService messageService) {
        this.toolExecutionService = toolExecutionService;
        this.modelFactory = modelFactory;
        this.auditService = auditService;
        this.messageService = messageService;
    }

    @Override
    public Mono<Void> executeWorkflow(
            UUID sessionId, _AiMode aiMode, AiMessage aiMessage, ChatMemory chatMemory,
            _ToolsRegistry toolRegistry, SystemMessage systemPrompt, FluxSink<String> sink, long requestStartTime
    ) {
        // Add the AI message (with tool requests) to memory
        try {
            chatMemory.add(aiMessage);
        } catch (Exception e) {
            log.error("Failed to add AI message to chat memory: {}", e.getMessage());
        }

        long latency = System.currentTimeMillis() - requestStartTime;
        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
        // Latency here is for the initial AI response that requested tools
        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, null, toolExecutionRequests);

        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.TOOL_EXECUTION_REQUEST)
                .details(toolExecutionRequests)
                .metrics(metrics)
                .build();

        return auditService.recordInteraction(auditRecord)
                .thenMany(Flux.fromIterable(aiMessage.toolExecutionRequests()))
                .concatMap(req -> toolExecutionService.execute(sessionId, req, toolRegistry, chatMemory))
                .collectList()
                .flatMap(toolResults -> {
                    // DEFENSIVE CHECK: Handle cases where the AI hallucinates tool usage
                    // for a handler that was not configured with tools.
                    if (toolRegistry == null) {
                        log.warn("AI attempted to use tools, but no tool registry is configured. Session: {}", sessionId);
                        // Inform the AI that no tools are available and re-prompt.
                        return Flux.fromIterable(aiMessage.toolExecutionRequests())
                                .map(req -> ToolExecutionResultMessage.from(req, "Error: No tools are available in the current context."))
                                .doOnNext(chatMemory::add)
                                .then(Mono.defer(() -> callModelAfterToolRequest(sessionId, aiMode, chatMemory, null, systemPrompt, sink)));
                    }

                    return callModelAfterToolRequest(sessionId, aiMode, chatMemory, toolRegistry, systemPrompt, sink);
                });
    }

    private Mono<Void> callModelAfterToolRequest(
            UUID sessionId, _AiMode aiMode, ChatMemory chatMemory,
            _ToolsRegistry toolRegistry, SystemMessage systemPrompt, FluxSink<String> sink
    ) {
        List<ChatMessage> newMessages = new ArrayList<>();
        if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
            newMessages.add(systemPrompt);
        }
        newMessages.addAll(chatMemory.messages());

        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(newMessages);

        if (toolRegistry != null) {
            builder.toolSpecifications(toolRegistry.getToolSpecifications());
        }

        ChatRequest afterToolRequest = builder.build();

        // Recursive call to the model
        return Mono.create(recursiveSink -> {
            _SimpleResponseProcessor simpleProcessor = new _SimpleResponseProcessor.Builder()
                    .sessionId(sessionId)
                    .chatMemory(chatMemory)
                    .auditService(auditService)
                    .messageService(messageService)
                    .build();

            _ToolResponseProcessor toolProcessor = new _ToolResponseProcessor.Builder()
                    .sessionId(sessionId)
                    .aiMode(aiMode)
                    .chatMemory(chatMemory)
                    .toolRegistry(toolRegistry)
                    .systemPrompt(systemPrompt)
                    .toolWorkflowFacade(this)
                    .sink(sink)
                    .requestStartTime(System.currentTimeMillis())
                    .build();
            _ToolResponseHandler handler = _ToolResponseHandler.builder()
                    .sink(sink)
                    .monoSink(recursiveSink)
                    .simpleProcessor(simpleProcessor)
                    .toolProcessor(toolProcessor)
                    .requestStartTime(System.currentTimeMillis())
                    .auditService(auditService)
                    .sessionId(sessionId)
                    .build();

            modelFactory.getModel(aiMode).chat(afterToolRequest, handler);
        });
    }
}