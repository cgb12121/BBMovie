package com.bbmovie.ai_assistant_service.core.low_level._entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_message")
public class _ChatMessage {

    @Id
    private Long id;

    @Column("session_id")
    private String sessionId;

    @Column("sender")
    private _Sender sender;

    // Json
    @Column("content")
    private String content;

    @Column("timestamp")
    private Instant timestamp;
}