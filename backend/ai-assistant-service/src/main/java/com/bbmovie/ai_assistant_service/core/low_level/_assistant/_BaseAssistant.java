package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
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

    protected _BaseAssistant(
            StreamingChatModel chatModel, ChatMemoryProvider chatMemoryProvider,
            _MessageService messageService, _AuditService auditService,
            _ToolRegistry toolRegistry, SystemMessage systemPrompt, AssistantMetadata metadata) {
        this.chatModel = chatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.messageService = messageService;
        this.auditService = auditService;
        this.toolRegistry = toolRegistry;
        this.systemPrompt = systemPrompt;
        this.metadata = metadata;
    }

    protected abstract _ChatResponseHandlerFactory getHandlerFactory();

    @Transactional  // Transaction boundary for full orchestration
    @Override
    public Flux<String> processMessage(UUID sessionId, String message, String userRole) {
        log.debug("[streaming] session={} type={} role={} message={}",
                sessionId, getType(), userRole, message);

        return auditService.recordInteraction(sessionId, _InteractionType.USER_MESSAGE, message)
                .then(messageService.saveUserMessage(sessionId, message))
                .flatMapMany(savedHistory -> {
                    log.debug("[streaming] User message saved (id={}), proceeding to AI chat.", savedHistory.getId());

                    ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(systemPrompt);
                    messages.addAll(chatMemory.messages());
                    messages.add(UserMessage.from(message));

                    ChatRequest request = ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(toolRegistry.getToolSpecifications())
                            .build();

                    return Flux.create((FluxSink<String> sink) ->
                            processChatRecursive(sessionId, request, sink)
                                    .doOnError(sink::error)
                                    .doOnSuccess(v -> sink.complete())
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe());
                })
                .doOnError(ex -> log.error("[streaming] Error in chat pipeline for session={}: {}",
                        sessionId, ex.getMessage(), ex));
    }

    private Mono<Void> processChatRecursive(UUID sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            chatModel.chat(chatRequest, getHandlerFactory().create(sessionId, chatMemory, sink, monoSink));
        });
    }
}
