package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.FileContentInfo;
import com.bbmovie.ai_assistant_service.dto.response.ChatMessageResponse;
import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    Mono<ChatMessage> saveUserMessage(UUID sessionId, String message);

    Mono<ChatMessage> saveUserMessageWithFiles(UUID sessionId, String message, List<String> fileReferences, String extractedContent, String fileContentType);

    Mono<ChatMessage> saveUserMessageWithFileContentInfo(UUID sessionId, String message, FileContentInfo fileContentInfo);

    Mono<ChatMessage> saveAiResponse(UUID sessionId, String content);

    Mono<ChatMessage> saveAiResponseWithFiles(UUID sessionId, String content, List<String> fileReferences, String extractedContent, String fileContentType);

    Mono<ChatMessage> saveAiResponseWithFileContentInfo(UUID sessionId, String content, FileContentInfo fileContentInfo);

    Mono<CursorPageResponse<ChatMessageResponse>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size);
}
