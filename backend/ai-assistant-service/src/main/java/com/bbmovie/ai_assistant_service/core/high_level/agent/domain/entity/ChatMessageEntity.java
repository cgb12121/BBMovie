package com.bbmovie.ai_assistant_service.core.high_level.agent.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "chat_messages")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity {

    @Id
    private Long id;

    @Column
    private String sessionId;

    @Column
    private MessageRole role; // USER or AI

    @Column
    private String content;

    @Column
    private String thinking;

    @Column
    private String toolUsage;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
