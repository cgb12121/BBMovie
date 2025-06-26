package com.example.bbmovie.security.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Log4j2
@Configuration
public class WebSocketDecoratorConfig {

    public static final String AUTH_ATTRIBUTE = "auth";

    @Bean
    public WebSocketHandlerDecoratorFactory authWebSocketHandlerDecorator() {
        return handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                        session.getAttributes().get(AUTH_ATTRIBUTE);

                if (auth != null) {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                    log.info("SecurityContext set for user: {}", auth.getName());

                    // Ensure the principal is set in the session
                    session.getAttributes().put("principal", auth);
                    session.getAttributes().put(AUTH_ATTRIBUTE, auth);
                } else {
                    log.warn("No authentication found in WebSocket session attributes");
                }

                super.afterConnectionEstablished(session);
            }
        };
    }
}
