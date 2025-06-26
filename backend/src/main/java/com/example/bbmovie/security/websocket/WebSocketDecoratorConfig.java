package com.example.bbmovie.security.websocket;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
public class WebSocketDecoratorConfig {

    @Bean
    public WebSocketHandlerDecoratorFactory authWebSocketHandlerDecorator() {
        return handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) session.getAttributes().get("auth");

                if (auth != null) {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                }

                super.afterConnectionEstablished(session);
            }
        };
    }
}
