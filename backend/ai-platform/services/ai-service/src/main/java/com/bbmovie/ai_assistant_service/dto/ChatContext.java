package com.bbmovie.ai_assistant_service.dto;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class ChatContext {
    UUID sessionId;
    String userId; // Added for HITL
    String message;
    AiMode aiMode;
    String userRole;
    List<String> fileReferences;
    String extractedFileContent;
    String internalApprovalToken; // Added for HITL
    String messageId; // Added for HITL binding
}
