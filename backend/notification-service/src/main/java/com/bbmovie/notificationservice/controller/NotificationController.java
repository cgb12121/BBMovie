package com.bbmovie.notificationservice.controller;

import com.bbmovie.notificationservice.controller.openapi.NotificationControllerOpenApi;
import com.bbmovie.notificationservice.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerOpenApi {

    private final SseService sseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String userId) {
        return sseService.createEmitter(userId);
    }

    // For testing: broadcast news to everyone connected
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody Map<String, Object> news) {
        sseService.broadcast(news);
    }
}
