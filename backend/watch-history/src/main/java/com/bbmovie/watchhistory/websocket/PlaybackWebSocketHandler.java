package com.bbmovie.watchhistory.websocket;

import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import com.bbmovie.watchhistory.service.WatchHistoryTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaybackWebSocketHandler extends TextWebSocketHandler {

    private final WatchHistoryTrackingService trackingService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get(JwtQueryHandshakeInterceptor.SESSION_USER_ID);
        if (!(userId instanceof String) || ((String) userId).isBlank()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            } catch (Exception e) {
                log.debug("Close after failed auth: {}", e.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object rawUser = session.getAttributes().get(JwtQueryHandshakeInterceptor.SESSION_USER_ID);
        if (!(rawUser instanceof String userId) || userId.isBlank()) {
            sendError(session, "Unauthorized");
            return;
        }
        PlaybackTrackRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), PlaybackTrackRequest.class);
        } catch (Exception e) {
            sendError(session, "Invalid JSON");
            return;
        }
        if (request.getMovieId() == null) {
            sendError(session, "movieId required");
            return;
        }
        if (request.getPositionSec() == null || request.getPositionSec() < 0) {
            sendError(session, "positionSec required");
            return;
        }
        try {
            TrackPlaybackResponse response = trackingService.track(userId, request);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.warn("WebSocket track failed: {}", e.getMessage());
            sendError(session, "Track failed");
        }
    }

    private void sendError(WebSocketSession session, String msg) throws Exception {
        Map<String, String> err = new HashMap<>();
        err.put("status", "error");
        err.put("message", msg);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
    }
}
