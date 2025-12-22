package com.bbmovie.ai_assistant_service.dto.response;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.ai_assistant_service.entity.Sender;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ChatMessageResponse {

    // Intentionally DO NOT expose internal Long id
    UUID sessionId;
    Sender sender;
    String content;
    Instant timestamp;
    String fileContentJson;

    public static ChatMessageResponse fromEntity(ChatMessage entity) {
        if (entity == null) {
            return null;
        }
        return ChatMessageResponse.builder()
                .sessionId(entity.getSessionId())
                .sender(entity.getSender())
                .content(entity.getContent())
                .timestamp(entity.getTimestamp())
                .fileContentJson(entity.getFileContentJson())
                .build();
    }
}


