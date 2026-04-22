package com.bbmovie.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "notification_type")
    private String type; // e.g., NEWS, ALERT, PROMO

    @Builder.Default
    @Column(name = "published_at")
    private LocalDateTime publishedAt = LocalDateTime.now();
}
