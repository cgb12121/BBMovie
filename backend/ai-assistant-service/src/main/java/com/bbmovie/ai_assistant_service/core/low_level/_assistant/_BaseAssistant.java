package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class _BaseAssistant implements _Assistant {

    private final _ModelFactory modelFactory;
    private final ChatMemoryProvider chatMemoryProvider;
    private final _MessageService messageService;
    private final _AuditService auditService;
    private final _ToolsRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final _AssistantMetadata metadata;
    private final _RagService ragService;

    protected _BaseAssistant(
            _ModelFactory modelFactory,
            ChatMemoryProvider chatMemoryProvider,
            _MessageService messageService,
            _AuditService auditService,
            _ToolsRegistry toolRegistry,
            SystemMessage systemPrompt,
            _AssistantMetadata metadata,
            _RagService ragService) {
        this.modelFactory = modelFactory;
        this.chatMemoryProvider = chatMemoryProvider;
        this.messageService = messageService;
        this.auditService = auditService;
        this.toolRegistry = toolRegistry;
        this.systemPrompt = systemPrompt;
        this.metadata = metadata;
        this.ragService = ragService;
    }

    protected abstract _ChatResponseHandlerFactory getHandlerFactory();

    @Transactional
    @Override
    public Flux<_ChatStreamChunk> processMessage(UUID sessionId, String message, _AiMode aiMode, String userRole) {
        long start = System.currentTimeMillis();
        log.debug("[streaming] session={} type={} role={} message={}", sessionId, getType(), userRole, message);

        return messageService.saveUserMessage(sessionId, message)
                .flatMap(savedMessage -> {
                    long latency = System.currentTimeMillis() - start;
                    String modelName = metadata.getModelName();
                    String toolName = metadata.getType().name();
                    _Metrics metrics = _MetricsUtil.get(latency, null, modelName, toolName);
                    _AuditRecord auditRecord = _AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(_InteractionType.USER_MESSAGE)
                            .details(message)
                            .metrics(metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(savedMessage);
                })
                .flatMapMany(savedMessage -> {
                    ChatRequest chatRequest = prepareChatRequest(sessionId, message);

                    return Flux.<String>create(sink ->
                                    processChatRecursive(sessionId, aiMode, chatRequest, sink)
                                            .doOnError(sink::error)
                                            .doOnSuccess(v -> sink.complete())
                                            .subscribeOn(Schedulers.boundedElastic()) // Offload the blocking AI call
                                            .subscribe()
                            )
                            .timeout(Duration.ofSeconds(45), Mono.error(new TimeoutException("AI response timed out after 45 seconds.")))
                            .map(_ChatStreamChunk::assistant)
                            .doOnComplete(() -> log.debug("[streaming] Stream completed for session {}", sessionId));
                })
                .onErrorResume(ex -> {
                    log.error("[streaming] Error in chat pipeline for session={}: {}", sessionId, ex.getMessage(), ex);
                    String errorMessage = ex instanceof java.util.concurrent.TimeoutException
                            ? "AI response timed out. Please try again."
                            : "Something went wrong. Please try again later.";
                    return Flux.just(_ChatStreamChunk.system(errorMessage));
                });
    }

    private ChatRequest prepareChatRequest(UUID sessionId, String message) {
        try {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            List<ChatMessage> passConversation = chatMemory.messages();
            List<ChatMessage> messages = new ArrayList<>();
            String finalMessage = "[Chat session:" + sessionId + "] " + message;

            messages.add(systemPrompt);

            messages.addAll(passConversation);
            messages.add(UserMessage.from(finalMessage));

            return ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(toolRegistry.getToolSpecifications())
                    .build();
        } catch (Exception e) {
            log.error("[prepareChatRequest] Failed to get memory: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Mono<Void> processChatRecursive(UUID sessionId, _AiMode aiMode, ChatRequest chatRequest, FluxSink<String> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            try {
                StreamingChatResponseHandler handler = getHandlerFactory()
                         .create(sessionId, chatMemory, sink, monoSink, aiMode);
                modelFactory.getModel(aiMode).chat(chatRequest, handler);
            } catch (Exception ex) {
                log.error("[streaming] chatModel.chat failed: {}", ex.getMessage(), ex);
                // Ensure both sinks are terminated on the initial error
                sink.error(ex);
                monoSink.error(ex);
            }
        });
    }
}

