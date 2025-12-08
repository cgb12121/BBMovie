package com.bbmovie.ai_assistant_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.bbmovie.ai_assistant_service.assistant.Assistant;
import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.service.ChatService;
import com.bbmovie.ai_assistant_service.service.FileProcessingService;
import com.bbmovie.ai_assistant_service.service.FileProcessingService.FileProcessingResult;
import com.bbmovie.ai_assistant_service.service.FileProcessingService.ProcessedFileContent;
import com.bbmovie.ai_assistant_service.service.SessionService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of ChatService handling both text-only and file-based chat requests.
 * <p>
 * For file-based requests, this service:
 * 1. Processes files (upload, transcription, text extraction) via FileProcessingService
 * 2. Builds a ChatRequestDto with file references and extracted content
 * 3. Delegates to the appropriate assistant for response generation
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ChatServiceImpl.class);

    private final Map<AssistantType, Assistant> assistants;
    private final SessionService sessionService;
    private final FileProcessingService fileProcessingService;

    @Autowired
    public ChatServiceImpl(
            List<Assistant> assistantList,
            SessionService sessionService,
            FileProcessingService fileProcessingService) {
        this.assistants = assistantList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Assistant::getType,
                        Function.identity())
                );
        this.sessionService = sessionService;
        this.fileProcessingService = fileProcessingService;
        log.info("Initialized ChatService with assistants: {}", this.assistants.keySet());
    }

    @Override
    public Flux<ChatStreamChunk> chat(UUID sessionId, ChatRequestDto request, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        String userRole = jwt.getClaimAsString(ROLE);

        // Process files if present in the request DTO
        Mono<FileProcessingResult> fileProcessingMono = hasAttachments(request)
                ? fileProcessingService.processAttachments(request.getAttachments())
                : Mono.just(FileProcessingResult.empty());

        return sessionService.getAndValidateSessionOwnership(sessionId, userId)
                .flatMapMany(session -> fileProcessingMono
                        .flatMapMany(fileResult -> {
                            // 1. Tạo chuỗi content
                            String contentStr = formatExtractedContent(fileResult.processedFiles());

                            // 2. Build Context trực tiếp (Không set vào request nữa)
                            ChatContext chatContext = ChatContext.builder()
                                    .sessionId(sessionId)
                                    .message(request.getMessage())
                                    .aiMode(request.getAiMode())
                                    .userRole(userRole)
                                    // Lấy từ fileResult mới nhất
                                    .fileReferences(fileResult.fileReferences())
                                    .extractedFileContent(contentStr)
                                    .build();

                            logFileProcessingResult(sessionId, fileResult);

                            return processChat(chatContext, request.getAssistantType());
                        }))
                .doOnError(error -> log.error("Chat error for session {}: {}", sessionId, error.getMessage()));
    }

    /**
     * Core chat processing logic shared by both text-only and file-based flows.
     */
    private Flux<ChatStreamChunk> processChat(ChatContext chatContext, String specifiedAssistantType) {
        AssistantType assistantType = AssistantType.fromCode(specifiedAssistantType);
        Assistant assistant = assistants.get(assistantType);
        
        if (assistant == null) {
            return Flux.error(new IllegalArgumentException("Unknown assistant type: " + assistantType));
        }

        if (chatContext.getAiMode() == AiMode.FAST || chatContext.getAiMode() == AiMode.NORMAL) {
            return assistant.processMessage(chatContext);
        }

        return assistant.processMessage(chatContext)
                .startWith(ChatStreamChunk.system("Assistant is thinking..."))
                .concatWithValues(ChatStreamChunk.system("Done"));
    }

    /**
     * Formats processed file content into a structured string for the LLM.
     * Each file's content is clearly labeled with filename, type, and extracted text.
     */
    private String formatExtractedContent(List<ProcessedFileContent> processedFiles) {
        if (processedFiles == null || processedFiles.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        for (ProcessedFileContent file : processedFiles) {
            content.append("=== File: ").append(file.filename()).append(" ===\n");
            content.append("Type: ").append(file.fileType()).append("\n");
            
            if (file.extractedText() != null && !file.extractedText().isBlank()) {
                content.append("Content:\n").append(file.extractedText()).append("\n");
            }
            content.append("\n");
        }
        return content.toString();
    }

    /**
     * Logs file processing results for debugging.
     */
    private void logFileProcessingResult(UUID sessionId, FileProcessingResult result) {
        log.info("Session {}: Processed {} files, {} with extracted content, {} references",
                sessionId,
                result.uploadedFiles().size(),
                result.processedFiles().size(),
                result.fileReferences().size());
    }

    private boolean hasAttachments(ChatRequestDto request) {
        return request.getAttachments() != null && !request.getAttachments().isEmpty();
    }
}