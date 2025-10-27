package com.bbmovie.ai_assistant_service._experimental._low_level.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_chat_history")
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _ChatHistory {

    @Id
    private Long id;

    @Column("session_id")
    private String sessionId;

    @Column("message_type")
    private String messageType; // e.g., "USER", "AI", "TOOL_REQUEST", "TOOL_RESULT"

    @Column("content")
    private String content;

    @Column("timestamp")
    private Instant timestamp;
}
