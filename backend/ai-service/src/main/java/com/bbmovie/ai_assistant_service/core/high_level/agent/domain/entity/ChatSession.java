package com.bbmovie.ai_assistant_service.core.high_level.agent.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "chat_sessions")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    private String id; //uuid

    @Column
    private String userId;

    @Column
    private String sessionName;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
