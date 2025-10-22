//package com.bbmovie.ai_assistant_service.domain.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "chat_messages")
//@Getter
//@Setter
//@ToString
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ChatMessageEntity {
//
//    @Id
//    private Long id;
//
//    @Column(nullable = false)
//    private Long sessionId;
//
//    @Enumerated(EnumType.STRING)
//    private MessageRole role; // USER or AI
//
//    @Column(columnDefinition = "TEXT")
//    private String content;
//
//    private LocalDateTime createdAt;
//}
