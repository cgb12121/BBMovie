package com.bbmovie.ai_assistant_service.assistant;

import com.bbmovie.ai_assistant_service.config.ai.ModelFactory;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.handler.ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.RagService;
import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseAssistant implements Assistant {

    private static final Logger log = LoggerFactory.getLogger(BaseAssistant.class);

    private final ModelFactory modelFactory;
    private final ChatMemoryProvider chatMemoryProvider;
    private final MessageService messageService;
    private final AuditService auditService;
    private final ToolsRegistry toolRegistry;
    private final SystemMessage systemPrompt;
    private final AssistantMetadata metadata;
    private final RagService ragService;

    protected abstract ChatResponseHandlerFactory getHandlerFactory();

    @Override
    public AssistantMetadata getInfo() {
        return this.metadata;
    }

    @Transactional
    @Override
    public Flux<ChatStreamChunk> processMessage(ChatContext context) {
        long start = System.currentTimeMillis();
        UUID sessionId = context.getSessionId();
        AiMode aiMode = context.getAiMode();
        String message = context.getMessage();
        String userRole = context.getUserRole();

        log.debug("[streaming] session={} type={} role={} message={}", sessionId, getType(), userRole, message);

        return messageService.saveUserMessage(context.getSessionId(), context.getMessage())
                .flatMap(savedMessage -> recordUserMessage(savedMessage, start, sessionId, message))
                .flatMapMany(savedMessage ->
                    prepareChatRequest(sessionId, message, userRole)
                        .flatMapMany(chatRequest -> processChatStream(chatRequest, sessionId, aiMode))
                )
                .onErrorResume(ex -> {
                    log.error("[streaming] Error in chat pipeline for session={}: {}", sessionId, ex.getMessage(), ex);
                    String errorMessage = ex instanceof TimeoutException
                            ? "AI response timed out. Please try again."
                            : "Something went wrong. Please try again later.";
                    return Flux.just(ChatStreamChunk.system(errorMessage));
                })
                .doOnError(ex -> log.error("[streaming] Unhandled error in stream for session={}: {}", sessionId, ex.getMessage()));
    }

    private Flux<ChatStreamChunk> processChatStream(ChatRequest chatRequest, UUID sessionId, AiMode aiMode) {
        return Flux.<ChatStreamChunk>create(sink ->
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
                .doOnComplete(() -> log.debug("[streaming] Stream completed for session {}", sessionId));
    }

    private Mono<Void> processChatRecursive(UUID sessionId, AiMode aiMode, ChatRequest chatRequest, FluxSink<ChatStreamChunk> sink) {
        return Mono.create(monoSink -> {
            ChatMemory chatMemory = chatMemoryProvider.get(sessionId.toString());
            try {
                StreamingChatResponseHandler handler = getHandlerFactory().create(sessionId, chatMemory, sink, monoSink, aiMode);
                modelFactory.getModel(aiMode).chat(chatRequest, handler);
            } catch (Exception ex) {
                log.error("[streaming] chatModel.chat failed: {}", ex.getMessage());
                // Ensure both sinks are terminated on the initial error
                sink.error(ex);
                monoSink.error(ex);
            }
        });
    }

    private Mono<ChatRequest> prepareChatRequest(UUID sessionId, String message, String userRole) {
        return Mono.fromCallable(() -> {
                    String finalMessage = "[User's role:{" + userRole + "} | User's chat session:{" + sessionId + "}] " + message;
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
                    // Fallback
                    log.error("[prepareChatRequest] Error in chat pipeline for session={}: {}", sessionId, e.getMessage());
                    ChatRequest request = ChatRequest.builder()
                            .messages(List.of(systemPrompt, UserMessage.from(message)))
                            .build();
                    return Mono.just(request);
                });
    }

    private Mono<com.bbmovie.ai_assistant_service.entity.ChatMessage> recordUserMessage(
            com.bbmovie.ai_assistant_service.entity.ChatMessage savedMessage, long start, UUID sessionId, String message) {
        long latency = System.currentTimeMillis() - start;
        String modelName = metadata.getModelName();
        String toolName = metadata.getType().name();
        Metrics metrics = MetricsUtil.get(latency, null, modelName, toolName);
        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(sessionId)
                .type(InteractionType.USER_MESSAGE)
                .details(message)
                .metrics(metrics)
                .build();
        return auditService.recordInteraction(auditRecord)
                .thenReturn(savedMessage);
    }
}