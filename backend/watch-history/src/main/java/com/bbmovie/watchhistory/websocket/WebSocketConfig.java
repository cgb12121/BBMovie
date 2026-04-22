package com.bbmovie.watchhistory.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Raw WebSocket: one TCP connection, you send JSON text frames yourself (like HTTP body over a pipe).
 * RSocket (see {@code PlaybackRsocketController} on port {@code spring.rsocket.server.port}) adds
 * framing, routes, metadata (e.g. JWT bearer), and reactive stream types (fire-and-forget, request-stream, …).
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PlaybackWebSocketHandler playbackWebSocketHandler;
    private final JwtQueryHandshakeInterceptor jwtQueryHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(playbackWebSocketHandler, "/api/watch-history/v1/ws")
                .addInterceptors(jwtQueryHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
