package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.RedisConfig;
import com.bbmovie.notificationservice.dto.SseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Service
@RequiredArgsConstructor
public class SseService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // User connects to this specific instance
    public SseEmitter createEmitter(String userId) {
        // Use a shorter timeout (60 seconds) as recommended
        SseEmitter emitter = new SseEmitter(60_000L); 
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        log.info("SSE emitter created for user: {} on this instance", userId);
        
        // Initial keep-alive or connection message
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }
        
        return emitter;
    }

    // Publish to Redis instead of sending directly, so all instances can hear
    public void sendNotification(String userId, Object notification) {
        sendToRedis(userId, notification, "notification");
    }

    public void broadcast(Object notification) {
        sendToRedis(null, notification, "news");
    }

    private void sendToRedis(String userId, Object notification, String eventName) {
        try {
            SseMessage message = SseMessage.builder()
                    .userId(userId)
                    .data(notification)
                    .eventName(eventName)
                    .build();
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisConfig.SSE_TOPIC, payload);
        } catch (Exception e) {
            log.error("Failed to publish SSE message to Redis", e);
        }
    }

    // This method is called by Redis MessageListenerAdapter (from RedisConfig)
    public void handleRedisMessage(String payload) {
        try {
            SseMessage sseMessage = objectMapper.readValue(payload, SseMessage.class);
            
            if (sseMessage.getUserId() == null) {
                // Broadcast to all emitters on this instance
                emitters.forEach((userId, emitter) -> sendToEmitter(emitter, userId, sseMessage));
            } else {
                // Targeted notification
                SseEmitter emitter = emitters.get(sseMessage.getUserId());
                if (emitter != null) {
                    sendToEmitter(emitter, sseMessage.getUserId(), sseMessage);
                }
            }
        } catch (Exception e) {
            log.error("Error handling Redis message for SSE", e);
        }
    }

    private void sendToEmitter(SseEmitter emitter, String userId, SseMessage sseMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name(sseMessage.getEventName())
                    .data(sseMessage.getData()));
            log.debug("Sent SSE event {} to user {} from this instance", sseMessage.getEventName(), userId);
        } catch (IOException e) {
            log.warn("Failed to send SSE to user {} from this instance, removing emitter", userId);
            emitters.remove(userId);
        }
    }
}
