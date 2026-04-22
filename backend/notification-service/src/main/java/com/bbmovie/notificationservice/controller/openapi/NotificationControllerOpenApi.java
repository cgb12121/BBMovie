package com.bbmovie.notificationservice.controller.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@SuppressWarnings("unused")
@Tag(name = "Notifications", description = "SSE notification streaming APIs")
public interface NotificationControllerOpenApi {
    @Operation(summary = "Open SSE stream")
    SseEmitter stream(@RequestParam String userId);

    @Operation(summary = "Broadcast notification")
    void broadcast(@RequestBody Map<String, Object> news);
}

