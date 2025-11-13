package com.bbmovie.ai_assistant_service.core.low_level._dto._response;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class _ChatSessionResponse {
    private UUID sessionId;
    private String sessionName;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isArchived;

    public static _ChatSessionResponse fromEntity(_ChatSession session) {
        return _ChatSessionResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionName())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .isArchived(session.isArchived())
                .build();
    }
}
