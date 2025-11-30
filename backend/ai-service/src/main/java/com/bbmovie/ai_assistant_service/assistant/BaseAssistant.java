package com.bbmovie.ai_assistant_service.assistant;

import com.bbmovie.ai_assistant_service.config.ai.ModelFactory;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.FileContentInfo;
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
import com.bbmovie.ai_assistant_service.utils.FileTypeUtils;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
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

    private static final RgbLogger log = RgbLoggerFactory.getLogger(BaseAssistant.class);

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
        UUID sessionId = context.getSessionId();
        AiMode aiMode = context.getAiMode();
        String message = context.getMessage();
        String userRole = context.getUserRole();

        log.debug("[streaming] session={} type={} role={} message={}", sessionId, getType(), userRole, message);

        // Save user message with file content if available
        Mono<com.bbmovie.ai_assistant_service.entity.ChatMessage> saveMessageMono;
        if (context.getFileReferences() != null &&
                (!context.getFileReferences().isEmpty() || context.getExtractedFileContent() != null)
        ) {
            FileContentInfo fileContentInfo = FileContentInfo.builder()
                    .fileReferences(context.getFileReferences())
                    .extractedContent(context.getExtractedFileContent())
                    .fileContentType(determineFileContentType(context.getFileReferences()))
                    .build();
            saveMessageMono = messageService.saveUserMessageWithFileContentInfo(
                    context.getSessionId(),
                    message,
                    fileContentInfo
            );
        } else {
            saveMessageMono = messageService.saveUserMessage(context.getSessionId(), message);
        }

        return saveMessageMono
                .flatMap(savedMessage -> recordUserMessage(savedMessage, sessionId, message))
                .flatMapMany(savedMessage -> prepareChatRequest(sessionId, context)
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

    protected Mono<ChatRequest> prepareChatRequest(UUID sessionId, ChatContext context) {
        return Mono.fromCallable(() -> {
                    String message = context.getMessage();
                    List<String> fileReferences = context.getFileReferences();
                    String extractedContent = context.getExtractedFileContent();

                    // Enhance the message with file context if files are present
                    String finalMessage = buildEnhancedMessage(
                            message, fileReferences, extractedContent, context.getUserRole(), sessionId
                    );
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
                            .messages(List.of(systemPrompt, UserMessage.from(context.getMessage())))
                            .build();
                    return Mono.just(request);
                });
    }

    private String buildEnhancedMessage(
            String message, List<String> fileReferences, String extractedContent, String userRole, UUID sessionId) {
        StringBuilder enhancedMessage = new StringBuilder();

        // Add role and session context
        enhancedMessage.append("[User's role:{").append(userRole).append("}")
                .append("|")
                .append("User's chat session:{").append(sessionId).append("}] ");

        // Add the original message
        enhancedMessage.append(message);

        // Add processed file information if any
        if (fileReferences != null && !fileReferences.isEmpty()) {
            enhancedMessage.append("\n\n--- ATTACHED FILES ---");
            for (int i = 0; i < fileReferences.size(); i++) {
                String fileUrl = fileReferences.get(i);
                String fileName = FileTypeUtils.extractFileName(fileUrl);
                String fileType = FileTypeUtils.extractFileType(fileUrl);

                enhancedMessage.append("\nFile ").append(i + 1).append(": ");
                enhancedMessage.append("Name: ").append(fileName).append(", ");
                enhancedMessage.append("Type: ").append(fileType).append("\n");

                // If it's an audio file, and we have extracted content, make it clear it's already processed
                if (FileTypeUtils.isAudioFile(fileUrl) && extractedContent != null && !extractedContent.trim().isEmpty()) {
                    enhancedMessage.append("Audio Content (already transcribed): ").append(extractedContent).append("\n");
                } else if (FileTypeUtils.isImageFile(fileUrl)) {
                    enhancedMessage.append("Image attached (available for visual analysis)\n");
                } else if (FileTypeUtils.isDocumentFile(fileUrl)) {
                    enhancedMessage.append("Document attached\n");
                } else {
                    enhancedMessage.append("File attached\n");
                }
            }
            enhancedMessage.append("--- END OF FILE INFO ---");
        }

        // Add any additional extracted content that wasn't tied to a specific file
        if (extractedContent != null && !extractedContent.isEmpty() && !FileTypeUtils.hasAudioFile(fileReferences)) {
            enhancedMessage.append("\n\n[ADDITIONAL_CONTENT]:\n").append(extractedContent);
        }

        return enhancedMessage.toString();
    }


    private String determineFileContentType(List<String> fileReferences) {
        return FileTypeUtils.determineFileContentType(fileReferences);
    }

    private Mono<com.bbmovie.ai_assistant_service.entity.ChatMessage> recordUserMessage(
            com.bbmovie.ai_assistant_service.entity.ChatMessage savedMessage, UUID sessionId, String message) {
        String modelName = metadata.getModelName();
        String toolName = metadata.getType().name();
        Metrics metrics = MetricsUtil.get(0, null, modelName, toolName);
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