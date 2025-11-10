package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @deprecated Legacy default response handler.
 * <p>
 * This God class will stay here for a while when the reworked response handlers work smoothly
 */
@Slf4j
@Deprecated(since = "1.0.0", forRemoval = true)
public class _DefaultResponseHandler extends _BaseResponseHandler {

    private final UUID sessionId;
    private final _AiMode aiMode;
    private final ChatMemory chatMemory;
    private final _ModelFactory modelFactory;
    private final SystemMessage systemPrompt;
    private final _ToolsRegistry toolRegistry;
    private final _MessageService messageService;
    private final _ToolExecutionService toolExecutionService;
    private final _AuditService auditService;
    private final long requestStartTime;

    // Private constructor that takes the Builder
    private _DefaultResponseHandler(Builder builder) {
        super(builder.sink, builder.monoSink);
        this.sessionId = builder.sessionId;
        this.aiMode = builder.aiMode;
        this.chatMemory = builder.chatMemory;
        this.modelFactory = builder.modelFactory;
        this.systemPrompt = builder.systemPrompt;
        this.toolRegistry = builder.toolRegistry;
        this.messageService = builder.messageService;
        this.toolExecutionService = builder.toolExecutionService;
        this.auditService = builder.auditService;
        this.requestStartTime = builder.requestStartTime;
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        long latency = System.currentTimeMillis() - requestStartTime;
        AiMessage aiMsg = completeResponse.aiMessage();
        ChatResponseMetadata metadata = completeResponse.metadata();

        Mono<Void> processMono;
        if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {
            processMono = handleToolExecution(aiMsg, latency, metadata);
        } else {
            processMono = handleSimpleResponse(aiMsg, latency, metadata);
        }

        processMono.then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }

    @Override
    public void onError(Throwable error) {
        long latency = System.currentTimeMillis() - requestStartTime;
        _Metrics metrics = _Metrics.builder().latencyMs(latency).build();
        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.ERROR)
                .details(error.getMessage())
                .metrics(metrics)
                .build();
        auditService.recordInteraction(auditRecord)
                .subscribe(
                        v -> log.debug("Error audit recorded"),
                        e -> log.error("Failed to record error audit", e)
                );
        super.onError(error);
    }

    private Mono<Void> handleSimpleResponse(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        chatMemory.add(aiMessage);
        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, metadata, aiMessage.toolExecutionRequests());
        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.AI_COMPLETE_RESULT)
                .details(aiMessage.text())
                .metrics(metrics)
                .build();
        Mono<Void> auditMono = auditService.recordInteraction(auditRecord);
        Mono<Void> saveMono = messageService.saveAiResponse(sessionId, aiMessage.text())
                .then();
        return Mono.when(auditMono.onErrorResume(e -> Mono.empty()), saveMono);
    }

    private Mono<Void> handleToolExecution(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        chatMemory.add(aiMessage);
        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, metadata, aiMessage.toolExecutionRequests());

        // DEFENSIVE CHECK: Handle cases where the AI hallucinates tool usage
        // for a handler that was not configured with tools.
        if (toolRegistry == null || toolExecutionService == null) {
            log.warn("AI attempted to use tools, but no tool registry is configured for this handler. Session: {}", sessionId);
            // Inform the AI that no tools are available and re-prompt.
            return Flux.fromIterable(aiMessage.toolExecutionRequests())
                    .map(req -> ToolExecutionResultMessage.from(req, "Error: No tools are available in the current context."))
                    .doOnNext(chatMemory::add)
                    .then(Mono.defer(() -> {
                        List<ChatMessage> newMessages = new ArrayList<>();
                        if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
                            newMessages.add(systemPrompt);
                        }
                        newMessages.addAll(chatMemory.messages());
                        ChatRequest afterToolRequest = ChatRequest.builder()
                                .messages(newMessages)
                                .build(); // No tool specifications are sent
                        return processToolRequestRecursively(afterToolRequest);
                    }));
        }

        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.TOOL_EXECUTION_REQUEST)
                .details(aiMessage.toolExecutionRequests())
                .metrics(metrics)
                .build();
        return auditService.recordInteraction(auditRecord)
                .then(Flux.fromIterable(aiMessage.toolExecutionRequests())
                        .concatMap(req -> toolExecutionService.execute(sessionId, req, toolRegistry, chatMemory))
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
                });
    }

    private Mono<Void> processToolRequestRecursively(ChatRequest chatRequest) {
        return Mono.create(recursiveSink ->
                modelFactory.getModel(this.aiMode).chat(
                        chatRequest,
                        toBuilder().requestStartTime(System.currentTimeMillis())
                                .monoSink(recursiveSink)
                                .build()
                )
        );
    }

    public Builder toBuilder() {
        return new Builder()
                .sessionId(this.sessionId)
                .aiMode(this.aiMode)
                .chatMemory(this.chatMemory)
                .modelFactory(this.modelFactory)
                .systemPrompt(this.systemPrompt)
                .toolRegistry(this.toolRegistry)
                .messageService(this.messageService)
                .toolExecutionService(this.toolExecutionService)
                .auditService(this.auditService)
                .sink(this.sink);
    }

    public static class Builder {
        private UUID sessionId;
        private _AiMode aiMode;
        private ChatMemory chatMemory;
        private _ModelFactory modelFactory;
        private SystemMessage systemPrompt;
        private _ToolsRegistry toolRegistry;
        private _MessageService messageService;
        private _ToolExecutionService toolExecutionService;
        private _AuditService auditService;
        private long requestStartTime;
        private FluxSink<String> sink;
        private MonoSink<Void> monoSink;

        public Builder sessionId(@NonNull UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder aiMode(@NonNull _AiMode aiMode) {
            this.aiMode = aiMode;
            return this;
        }

        public Builder chatMemory(@NonNull ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder modelFactory(@NonNull _ModelFactory modelFactory) {
            this.modelFactory = modelFactory;
            return this;
        }

        public Builder systemPrompt(@NonNull SystemMessage systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder toolRegistry(@Nullable _ToolsRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }

        public Builder messageService(@NonNull _MessageService messageService) {
            this.messageService = messageService;
            return this;
        }

        public Builder toolExecutionService(@Nullable _ToolExecutionService toolExecutionService) {
            this.toolExecutionService = toolExecutionService;
            return this;
        }

        public Builder auditService(@NonNull _AuditService auditService) {
            this.auditService = auditService;
            return this;
        }

        public Builder requestStartTime(long requestStartTime) {
            this.requestStartTime = requestStartTime;
            return this;
        }

        public Builder sink(@NonNull FluxSink<String> sink) {
            this.sink = sink;
            return this;
        }

        public Builder monoSink(@NonNull MonoSink<Void> monoSink) {
            this.monoSink = monoSink;
            return this;
        }

        public _DefaultResponseHandler build() {
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(aiMode, "aiMode must not be null");
            Objects.requireNonNull(chatMemory, "chatMemory must not be null");
            Objects.requireNonNull(modelFactory, "modelFactory must not be null");
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(messageService, "messageService must not be null");
            Objects.requireNonNull(auditService, "auditService must not be null");
            Objects.requireNonNull(sink, "sink must not be null");
            Objects.requireNonNull(monoSink, "monoSink must not be null");

            return new _DefaultResponseHandler(this);
        }
    }
}
