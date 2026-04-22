package com.bbmovie.notificationservice.controller.openapi;

import com.bbmovie.notificationservice.entity.NotificationPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Notification Preferences", description = "Notification preference APIs")
public interface NotificationPreferenceControllerOpenApi {
    @Operation(summary = "Save preference")
    NotificationPreference savePreference(@RequestBody NotificationPreference preference);

    @Operation(summary = "Get preference by user ID")
    NotificationPreference getPreference(@PathVariable String userId);
}

