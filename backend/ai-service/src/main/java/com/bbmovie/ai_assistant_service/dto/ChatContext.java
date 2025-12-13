package com.bbmovie.ai_assistant_service.dto;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ChatContext {
    UUID sessionId;
    String message;
    AiMode aiMode;
    String userRole;
    List<String> fileReferences;
    String extractedFileContent;
}