package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._config._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class _ToolExecutingResponseHandler extends _BaseResponseHandler {

    private final UUID sessionId;
    private final ChatMemory chatMemory;
    private final StreamingChatModel chatModel;
    private final SystemMessage systemPrompt;
    private final _ToolsRegistry toolRegistry;
    private final _MessageService messageService;
    private final _ToolExecutionService toolExecutionService;
    private final _AuditService auditService;
    // Track timing for this specific response
    private final long requestStartTime;

    public _ToolExecutingResponseHandler(
            UUID sessionId, ChatMemory chatMemory, FluxSink<String> sink, MonoSink<Void> monoSink,
            StreamingChatModel chatModel, SystemMessage systemPrompt, _ToolsRegistry toolRegistry,
            _MessageService messageService, _ToolExecutionService toolExecutionService,
            _AuditService auditService, long requestStartTime) {
        super(sink, monoSink);
        this.sessionId = sessionId;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
        this.toolRegistry = toolRegistry;
        this.messageService = messageService;
        this.toolExecutionService = toolExecutionService;
        this.auditService = auditService;
        this.requestStartTime = requestStartTime;
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        long latency = System.currentTimeMillis() - requestStartTime;
        AiMessage aiMsg = completeResponse.aiMessage();
        ChatResponseMetadata metadata = completeResponse.metadata();

        if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {
            handleToolExecution(aiMsg, latency, metadata);
        } else {
            handleSimpleResponse(aiMsg, latency, metadata);
        }
    }

    @Override
    public void onError(Throwable error) {
        long latency = System.currentTimeMillis() - requestStartTime;

        _Metrics metrics = _Metrics.builder()
                .latencyMs(latency)
                .build();

        auditService.recordInteraction(sessionId, _InteractionType.ERROR, error.getMessage(), metrics)
                .subscribe(
                        v -> log.debug("Error audit recorded"),
                        e -> log.error("Failed to record error audit", e)
                );

        super.onError(error);
    }

    private void handleSimpleResponse(AiMessage aiMsg, long latency, ChatResponseMetadata metadata) {
        chatMemory.add(aiMsg);

        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, metadata, aiMsg.toolExecutionRequests());

        // Record audit and save the message in parallel
        Mono<Void> auditMono = auditService.recordInteraction(
                sessionId,
                _InteractionType.AI_COMPLETE_RESULT,
                aiMsg.text(),
                metrics
        );

        Mono<Void> saveMono = messageService.saveAiResponse(sessionId, aiMsg.text()).then();

        Mono.when(
                auditMono.onErrorResume(e -> {
                            log.error("Audit failed but continuing", e);
                            return Mono.empty();
                        }),
                        saveMono
        )
        .then(Mono.fromRunnable(monoSink::success))
        .doOnError(monoSink::error)
        .subscribe();
    }

    private void handleToolExecution(AiMessage aiMsg, long latency, ChatResponseMetadata metadata) {
        chatMemory.add(aiMsg);

        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, metadata, aiMsg.toolExecutionRequests());

        // Record tool request audit
        auditService.recordInteraction(
                        sessionId,
                        _InteractionType.TOOL_EXECUTION_REQUEST,
                        aiMsg.toolExecutionRequests(),
                        metrics
                )
                .then(Flux.fromIterable(aiMsg.toolExecutionRequests())
                        .concatMap(req ->
                                toolExecutionService.execute(sessionId, req, toolRegistry, chatMemory)
                        )
                        .collectList()
                )
                .flatMap(toolResults -> {
                    List<ChatMessage> newMessages = new ArrayList<>();
                    if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
                        newMessages.add(systemPrompt);
                    }
                    newMessages.addAll(chatMemory.messages());

                    ChatRequest afterToolRequest = ChatRequest.builder()
                            .messages(newMessages)
                            .toolSpecifications(toolRegistry.getToolSpecifications())
                            .build();

                    return processToolRequestRecursively(afterToolRequest);
                })
                .then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }

    private Mono<Void> processToolRequestRecursively(ChatRequest chatRequest) {
        return Mono.create(recursiveSink ->
                chatModel.chat(chatRequest, new _ToolExecutingResponseHandler(
                        sessionId, chatMemory, sink, recursiveSink,
                        chatModel, systemPrompt, toolRegistry,
                        messageService, toolExecutionService, auditService,
                        System.currentTimeMillis()
                ))
        );
    }
}