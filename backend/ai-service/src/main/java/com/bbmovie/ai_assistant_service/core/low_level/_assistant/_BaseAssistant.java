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
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.AccessLevel;
import lombok.Getter;
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

@Getter(AccessLevel.PROTECTED)
public abstract class _BaseAssistant implements _Assistant {

    private static final _Logger log = _LoggerFactory.getLogger(_BaseAssistant.class);

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
                .flatMapMany(savedMessage ->
                    prepareChatRequest(sessionId, message)
                        .flatMapMany(chatRequest ->
                            Flux.<_ChatStreamChunk>create(sink ->
                                            processChatRecursive(sessionId, aiMode, chatRequest, sink)
                                                    .doOnError(sink::error)
                                                    .doOnSuccess(v -> sink.complete())
                                                    .subscribeOn(Schedulers.boundedElastic()) // Offload the blocking AI call
                                                    .subscribe()
                                    )
                                    .timeout(
                                            Duration.ofSeconds(45),
                                            Mono.error(new TimeoutException("AI response timed out after 45 seconds."))
                                    )
                                    .doOnComplete(() -> log.debug("[streaming] Stream completed for session {}", sessionId))
                        )
                )
                .onErrorResume(ex -> {
                    log.error("[streaming] Error in chat pipeline for session={}: {}", sessionId, ex.getMessage(), ex);
                    String errorMessage = ex instanceof TimeoutException
                            ? "AI response timed out. Please try again."
                            : "Something went wrong. Please try again later.";
                    return Flux.just(_ChatStreamChunk.system(errorMessage));
                })
                .doOnError(ex -> log.error("[streaming] Unhandled error in stream for session={}: {}", sessionId, ex.getMessage(), ex));
    }

    private Mono<ChatRequest> prepareChatRequest(UUID sessionId, String message) {
        return Mono.fromCallable(() -> {
                    String finalMessage = "[User's chat session:" + sessionId + "] " + message;
                    UserMessage userMessage = UserMessage.from(finalMessage);

                    ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
                    if (chatMemory == null) {
                        return ChatRequest.builder()
                                .messages(List.of(systemPrompt, userMessage))
                                .build();
                    }

                    List<ChatMessage> passConversation = chatMemory.messages();
                    List<ChatMessage> messages = new ArrayList<>();

                    messages.add(systemPrompt);
                    messages.addAll(passConversation);
                    messages.add(userMessage);

                    ChatRequest.Builder messagesBuilder = ChatRequest.builder()
                            .messages(messages);

                    if (toolRegistry != null) {
                        messagesBuilder.toolSpecifications(toolRegistry.getToolSpecifications());
                    }

                    chatMemory.add(userMessage); // Add to the chat memory

                    return messagesBuilder.build();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[prepareChatRequest] Error in chat pipeline for session={}: {}", sessionId, e.getMessage());
                    return Mono.just(
                            ChatRequest.builder()
                                    .messages(List.of(systemPrompt, UserMessage.from(message)))
                                    .build()
                    );
                });
    }

    private Mono<Void> processChatRecursive(UUID sessionId, _AiMode aiMode, ChatRequest chatRequest, FluxSink<_ChatStreamChunk> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            try {
                StreamingChatResponseHandler handler = getHandlerFactory()
                        .create(sessionId, chatMemory, sink, monoSink, aiMode);
                modelFactory.getModel(aiMode).chat(chatRequest, handler);
            } catch (Exception ex) {
                log.error("[streaming] chatModel.chat failed: {}", ex.getMessage());
                // Ensure both sinks are terminated on the initial error
                sink.error(ex);
                monoSink.error(ex);
            }
        });
    }
}

