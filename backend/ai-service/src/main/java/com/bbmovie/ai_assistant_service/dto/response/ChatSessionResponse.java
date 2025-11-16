package com.bbmovie.ai_assistant_service.dto.response;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionResponse {
    private UUID sessionId;
    private String sessionName;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isArchived;

    public static ChatSessionResponse fromEntity(ChatSession session) {
        return ChatSessionResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionName())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .isArchived(session.isArchived())
                .build();
    }
}
