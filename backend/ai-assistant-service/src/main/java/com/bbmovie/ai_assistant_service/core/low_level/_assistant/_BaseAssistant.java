package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagMovieDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._rag._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
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
    private final _ToolRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final AssistantMetadata metadata;
    private final _RagService ragService;

    protected _BaseAssistant(
            StreamingChatModel chatModel, ChatMemoryProvider chatMemoryProvider,
            _MessageService messageService, _AuditService auditService,
            _ToolRegistry toolRegistry, SystemMessage systemPrompt,
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
        log.debug("[streaming] session={} type={} role={} message={}", sessionId, getType(), userRole, message);

        return ragService.retrieveMovieContext(sessionId, message, 5)
                .flatMapMany(ragResult -> {
                    final List<_RagMovieDto> movieDocs = ragResult.documents().stream()
                            .filter(doc -> doc.getTitle() != null && doc.getPoster() != null)
                            .toList();
                    final String contextSummary = ragResult.summaryText();
                    final boolean hasContext = contextSummary != null && !contextSummary.isBlank();
                    final String enrichedMessage = hasContext
                            ? message + "\n\n[Movie Context]\n" + contextSummary
                            : message;

                    // Fire & forget index
                    ragService.indexConversationFragment(sessionId, message, movieDocs)
                            .onErrorResume(ex -> {
                                log.warn("[rag] Indexing skipped due to: {}", ex.getMessage());
                                return Mono.empty();
                            })
                            .subscribe();

                    // Record audit + save a user message
                    return auditService.recordInteraction(sessionId, _InteractionType.USER_MESSAGE, message)
                            .then(messageService.saveUserMessage(sessionId, enrichedMessage))
                            .flatMapMany(savedHistory -> {
                                log.debug("[streaming] User message saved (id={}), proceeding to AI chat.", savedHistory.getId());

                                ChatRequest chatRequest = prepareChatRequest(sessionId, contextSummary, enrichedMessage);

                                Flux<_ChatStreamChunk> ragChunk = movieDocs.isEmpty()
                                        ? Flux.empty()
                                        : Flux.just(
                                        _ChatStreamChunk.system("🎬 Movie context retrieved."),
                                        _ChatStreamChunk.ragResult(movieDocs)
                                );

                                Flux<_ChatStreamChunk> aiChunks = Flux.<String>create(sink ->
                                                processChatRecursive(sessionId, chatRequest, sink)
                                                        .doOnError(sink::error)
                                                        .doOnSuccess(v -> sink.complete())
                                                        .subscribeOn(Schedulers.boundedElastic())
                                                        .subscribe())
                                        .map(_ChatStreamChunk::assistant);

                                // Emit RAG results first, then AI stream
                                return ragChunk.concatWith(aiChunks);
                            });
                })
                .onErrorResume(ex -> {
                    log.error("[streaming] Error in chat pipeline for session={}: {}", sessionId, ex.getMessage(), ex);
                    return Flux.just(_ChatStreamChunk.system("❌ Something went wrong: " + ex.getMessage()));
                });
    }


    private ChatRequest prepareChatRequest(UUID sessionId, String contextSummary, String enrichedMessage) {
        ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(systemPrompt);

        if (contextSummary != null && !contextSummary.isBlank()) {
            SystemMessage summarizedContext = SystemMessage.from("Relevant movie context:\n" + contextSummary);
            messages.add(summarizedContext);
        }

        messages.addAll(chatMemory.messages());
        messages.add(UserMessage.from(enrichedMessage));

        return ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolRegistry.getToolSpecifications())
                .build();
    }

    private Mono<Void> processChatRecursive(UUID sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            chatModel.chat(chatRequest, getHandlerFactory().create(sessionId, chatMemory, sink, monoSink));
        });
    }
}

