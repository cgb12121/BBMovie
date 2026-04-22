package com.bbmovie.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryTask {
    private String userId;
    private String email;
    private String title;
    private String content;
    private String type;
    private Object payload; // Any extra data
}
