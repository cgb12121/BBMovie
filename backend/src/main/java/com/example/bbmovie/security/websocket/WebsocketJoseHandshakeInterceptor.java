package com.example.bbmovie.config.websocket;

import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.jose.JoseProviderStrategyContext;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JoseProviderStrategy joseProviderStrategy;

    @Autowired
    public JwtHandshakeInterceptor(JoseProviderStrategyContext joseProviderStrategyContext) {
        this.joseProviderStrategy = joseProviderStrategyContext.getActiveProvider();
    }

    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes
    ) {
        try {
            String token = extractToken(request);

            if (token != null && joseProviderStrategy.validateToken(token)) {
                String username = joseProviderStrategy.getUsernameFromToken(token);
                List<String> roles = joseProviderStrategy.getRolesFromToken(token);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, roles.stream().map(SimpleGrantedAuthority::new).toList());

                attributes.put("auth", auth); // passed into WebSocketSession later
                return true;
            }
        } catch (Exception e) {
            log.warn("WebSocket JWT authentication failed: {}", e.getMessage());
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }

        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query != null && query.contains("token=")) {
            return Arrays.stream(query.split("&"))
                    .filter(p -> p.startsWith("token="))
                    .map(p -> p.substring(6))
                    .findFirst().orElse(null);
        }

        return null;
    }

    @Override
    public void afterHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Nothing
    }
}
