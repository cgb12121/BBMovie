package com.bbmovie.ai_assistant_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.service.ChatService;
import com.bbmovie.ai_assistant_service.service.FileProcessingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.bbmovie.ai_assistant_service.entity.model.AiMode.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final FileProcessingService fileProcessingService;

    @PostMapping(value = "/{sessionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamChunk>> streamChatWithFiles(
            @PathVariable UUID sessionId,
            @RequestPart("message") String message,
            @RequestPart("assistantType") String assistantType,
            @RequestPart("aiMode") String aiMode,
            @RequestPart(value = "files", required = false) List<FilePart> files,
            @AuthenticationPrincipal Jwt jwt) {

        // Process files first (upload, transcribe, etc.)
        Mono<FileProcessingService.FileProcessingResult> fileProcessingMono = (files != null && !files.isEmpty())
                ? fileProcessingService.processFiles(files, jwt)
                : Mono.just(new FileProcessingService.FileProcessingResult(List.of(), List.of(), List.of()));

        return fileProcessingMono
                .flatMapMany(fileResult -> {
                    // Build ChatRequestDto with file references
                    ChatRequestDto request = new ChatRequestDto();
                    request.setMessage(message);
                    request.setAssistantType(assistantType);
                    request.setAiMode(valueOf(aiMode));
                    request.setFileReferences(fileResult.fileReferences());

                    // Format processed file content for the LLM
                    StringBuilder extractedContent = new StringBuilder();
                    if (fileResult.processedFiles() != null && !fileResult.processedFiles().isEmpty()) {
                        for (FileProcessingService.ProcessedFileContent content : fileResult.processedFiles()) {
                            extractedContent.append("[File: ").append(content.filename()).append("]\n");
                            extractedContent.append("[Type: ").append(content.fileType()).append("]\n");
                            extractedContent.append("[Content]:\n").append(content.extractedText()).append("\n\n");
                        }
                    }
                    request.setExtractedFileContent(extractedContent.toString());

                    // If there's extracted content, append it to the message so LLM sees it
                    // REMOVED: BaseAssistant already handles this injection to avoid duplication
                    // of (!extractedContent.isEmpty()) {
                    // request.setMessage(message + "\n\n" + extractedContent);
                    // }
                    log.info("request: {}", request);

                    return chatService.chat(sessionId, request, jwt);
                })
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    /**
     * Stream chat endpoint for text-only requests (original endpoint, backward
     * compatible).
     * This endpoint accepts JSON and doesn't handle files.
     */
    @PostMapping(value = "/{sessionId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId, request, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    // blocked stream for better postman view testing
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId, request, jwt)
                .collectList()
                .flatMap(chunks -> returnDebugResponse(sessionId, chunks));
    }

    private static Mono<Map<String, Object>> returnDebugResponse(UUID sessionId, List<ChatStreamChunk> chunks) {
        String combinedContent = chunks.stream()
                .filter(chunk -> "assistant".equals(chunk.getType()))
                .map(ChatStreamChunk::getContent)
                .collect(Collectors.joining());
        Map<String, Object> metadata = chunks.stream()
                .filter(chunk -> "assistant".equals(chunk.getType()))
                .map(ChatStreamChunk::getMetadata)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b));

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId.toString());
        response.put("type", "assistant");
        response.put("content", combinedContent);
        if (!metadata.isEmpty()) {
            response.put("metadata", metadata);
        }

        return Mono.just(response);
    }
}
