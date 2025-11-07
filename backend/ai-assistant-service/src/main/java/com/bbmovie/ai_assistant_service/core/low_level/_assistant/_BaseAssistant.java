package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantMetadata;
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
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
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

@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class _BaseAssistant implements _Assistant {

    private final StreamingChatModel chatModel;
    private final ChatMemoryProvider chatMemoryProvider;
    private final _MessageService messageService;
    private final _AuditService auditService;
    private final _ToolsRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final AssistantMetadata metadata;
    private final _RagService ragService;

    protected _BaseAssistant(
            StreamingChatModel chatModel, ChatMemoryProvider chatMemoryProvider,
            _MessageService messageService, _AuditService auditService,
            _ToolsRegistry toolRegistry, SystemMessage systemPrompt,
            AssistantMetadata metadata, _RagService ragService) {
        this.chatModel = chatModel;
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
    public Flux<_ChatStreamChunk> processMessage(UUID sessionId, String message, String userRole) {
        long start = System.currentTimeMillis();
        log.debug("[streaming] session={} type={} role={} message={}", sessionId, getType(), userRole, message);

        Mono<ChatRequest> chatRequestMono = Mono.fromCallable(() -> {
                    log.debug("[memory] Preparing chat request on blocking thread...");
                    // This is where getMessages() -> blockOptional() will be called
                    return prepareChatRequest(sessionId, message);
                })
                // Tell Reactor to run this *specific* call on a "blocking-safe" thread pool
                .subscribeOn(Schedulers.boundedElastic());

        Flux<_ChatStreamChunk> aiChunks = chatRequestMono.flatMapMany(chatRequest ->
                Flux.<String>create(sink ->
                                processChatRecursive(sessionId, chatRequest, sink)
                                        .doOnError(sink::error)
                                        .doOnSuccess(v -> sink.complete())
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe()
                        )
                        .map(_ChatStreamChunk::assistant)
                        .doOnComplete(() -> log.debug("[streaming] Stream completed for session {}", sessionId))
        );

        // After the stream finishes → audit + save message
        long latency = System.currentTimeMillis() - start;
        _Metrics metrics = _MetricsUtil.get(latency, null,
                metadata.getModelName(), metadata.getType().name());

        Mono<Void> finalizeActions = Mono.when(
                auditService.recordInteraction(sessionId, _InteractionType.USER_MESSAGE, message, metrics),
                messageService.saveUserMessage(sessionId, message)
        ).doOnSuccess(v -> log.debug("[audit] Saved chat & audit after stream completion"));

        return aiChunks.concatWith(finalizeActions.thenMany(Flux.empty()))
                .onErrorResume(ex -> {
                    log.error("[streaming] Error in chat pipeline for session={}: {}", sessionId, ex.getMessage(), ex);
                    return Flux.just(_ChatStreamChunk.system("Something went wrong. Please try again later."));
                });
    }

    private ChatRequest prepareChatRequest(UUID sessionId, String message) {
        try {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            List<ChatMessage> messages = new ArrayList<>();
            String finalMessage = "[Chat session:" + sessionId + "] " + message;

            messages.add(systemPrompt);

            messages.addAll(chatMemory.messages());
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

    private Mono<Void> processChatRecursive(UUID sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        return Mono.<Void>create(monoSink -> {
                    ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
                    try {
                        chatModel.chat(chatRequest, getHandlerFactory().create(sessionId, chatMemory, sink, monoSink));
                    } catch (Exception ex) {
                        log.error("[streaming] chatModel.chat failed: {}", ex.getMessage(), ex);
                        sink.error(ex);
                        monoSink.error(ex);
                    }
                })
                .timeout(Duration.ofSeconds(30)) // prevent infinite hang
                .onErrorResume(ex -> {
                    sink.next("[AI system timeout or unavailable]");
                    sink.complete();
                    return Mono.empty();
                })
                .doFinally(signal -> {
                    if (!sink.isCancelled()) sink.complete();
                });
    }
}

