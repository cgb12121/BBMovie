package com.bbmovie.ai_assistant_service.core.low_level._entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_message")
public class _ChatMessage {

    @Id
    private Long id;

    @Column("session_id")
    private UUID sessionId;

    @Column("sender")
    private _Sender sender;

    @Column("content")
    private String content;

    @CreatedDate
    @Column("timestamp")
    private Instant timestamp;
}