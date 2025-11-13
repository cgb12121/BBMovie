package com.bbmovie.common.dtos.nats;

import com.bbmovie.common.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String userId;
    private NotificationType type;
    private String destination;
    private String message;
}