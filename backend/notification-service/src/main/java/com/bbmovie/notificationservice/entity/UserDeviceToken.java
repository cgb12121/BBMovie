package com.bbmovie.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_device_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;
}
