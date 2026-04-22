package com.bbmovie.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTriggerEvent {
    private String type; // NEWS, MOVIE_RELEASE, etc.
    private String title;
    private String content;
    private Map<String, Object> metadata;
}
