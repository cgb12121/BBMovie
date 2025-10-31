package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatMessageService;
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
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class _BaseAssistant implements _Assistant {

    private final StreamingChatModel chatModel;
    private final ChatMemoryProvider chatMemoryProvider;
    private final _ChatMessageService chatMessageService;
    private final _ToolRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final AssistantMetadata metadata;

    protected _BaseAssistant(StreamingChatModel chatModel, ChatMemoryProvider chatMemoryProvider, _ChatMessageService chatMessageService, _ToolRegistry toolRegistry, SystemMessage systemPrompt, AssistantMetadata metadata) {
        this.chatModel = chatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.chatMessageService = chatMessageService;
        this.toolRegistry = toolRegistry;
        this.systemPrompt = systemPrompt;
        this.metadata = metadata;
    }

    protected abstract _ChatResponseHandlerFactory getHandlerFactory();

    @Override
    public AssistantMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public Flux<String> processMessage(String sessionId, String message, String userRole) {
        log.info("[streaming] session={} type={} role={} message={}",
                sessionId, getType(), userRole, message);

        return chatMessageService.saveUserMessage(sessionId, message)
                .log()
                .flatMapMany(savedHistory -> {
                    log.info("[streaming] User message saved (id={}), proceeding to AI chat.", savedHistory.getId());

                    ChatMemory chatMemory = chatMemoryProvider.get(sessionId);
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

    private Mono<Void> processChatRecursive(String sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId);
            chatModel.chat(chatRequest, getHandlerFactory().create(sessionId, chatMemory, sink, monoSink));
        });
    }
}
