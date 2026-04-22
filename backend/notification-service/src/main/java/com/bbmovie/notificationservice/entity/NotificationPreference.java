package com.bbmovie.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Builder.Default
    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "web_enabled")
    private boolean webEnabled = true;

    @Builder.Default
    @Column(name = "push_enabled")
    private boolean pushEnabled = true;
}
