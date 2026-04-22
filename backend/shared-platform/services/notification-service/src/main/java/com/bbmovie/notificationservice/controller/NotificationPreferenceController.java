package com.bbmovie.notificationservice.controller;

import com.bbmovie.notificationservice.controller.openapi.NotificationPreferenceControllerOpenApi;
import com.bbmovie.notificationservice.entity.NotificationPreference;
import com.bbmovie.notificationservice.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController implements NotificationPreferenceControllerOpenApi {

    private final NotificationPreferenceRepository preferenceRepository;

    @PostMapping
    public NotificationPreference savePreference(@RequestBody NotificationPreference preference) {
        return preferenceRepository.save(preference);
    }

    @GetMapping("/{userId}")
    public NotificationPreference getPreference(@PathVariable String userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(NotificationPreference.builder().userId(userId).build()));
    }
}
