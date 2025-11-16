package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.service.ChatService;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    @PostMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());
        return chatService.chat(sessionId, request.getMessage(), request.getAiMode(), assistantType, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }

    // blocked stream for better postman view testing
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());

        return chatService.chat(sessionId, request.getMessage(), request.getAiMode(), assistantType, jwt)
                .collectList()
                .flatMap(chunks -> returnDebugResponse(sessionId, chunks));
    }

    private static Mono<Map<String, Object>> returnDebugResponse(UUID sessionId, List<ChatStreamChunk> chunks) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chunks);
            log.info("Non streaming chat for session {}: {}", sessionId, prettyJson);
        } catch (JsonProcessingException e) {
            log.error("Error converting chunks to pretty json: {}", e.getMessage());
        }
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
                        (a, b) -> b)
                );
        String thinking = chunks.stream()
                .filter(chunk -> "thinking".equals(chunk.getType()))
                .map(ChatStreamChunk::getThinking)
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId.toString());
        response.put("type", "assistant");
        response.put("thinking", thinking);
        response.put("content", combinedContent);
        if (!metadata.isEmpty()) {
            response.put("metadata", metadata);
        }

        return Mono.just(response);
    }
}
