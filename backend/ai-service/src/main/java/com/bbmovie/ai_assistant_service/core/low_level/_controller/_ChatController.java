package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._request._ChatRequestDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
public class _ChatController {

    private static final _Logger log = _LoggerFactory.getLogger(_ChatController.class);

    private final _ChatService chatService;

    @Autowired
    public _ChatController(_ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<_ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        _AssistantType assistantType = _AssistantType.fromCode(request.getAssistantType());
        return chatService.chat(sessionId, request.getMessage(), request.getAiMode(), assistantType, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }

    // blocked stream for better postman view testing
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        _AssistantType assistantType = _AssistantType.fromCode(request.getAssistantType());

        return chatService.chat(sessionId, request.getMessage(), request.getAiMode(), assistantType, jwt)
                .collectList()
                .flatMap(chunks -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chunks);
                        log.info("Non streaming chat for session {}: {}", sessionId, prettyJson);
                    } catch (JsonProcessingException e) {
                        log.error("Error converting chunks to pretty json: {}", e.getMessage());
                    }
                    String combinedContent = chunks.stream()
                            .filter(chunk -> "assistant".equals(chunk.getType()))
                            .map(_ChatStreamChunk::getContent)
                            .collect(Collectors.joining());
                    Map<String, Object> metadata = chunks.stream()
                            .filter(chunk -> "assistant".equals(chunk.getType()))
                            .map(_ChatStreamChunk::getMetadata)
                            .flatMap(m -> m.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> b)
                            );
                    String thinking = chunks.stream()
                            .filter(chunk -> "thinking".equals(chunk.getType()))
                            .map(_ChatStreamChunk::getThinking)
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
                });
    }
}
