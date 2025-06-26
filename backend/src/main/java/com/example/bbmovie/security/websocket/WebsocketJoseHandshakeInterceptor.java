package com.example.bbmovie.security.websocket;

import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.jose.JoseProviderStrategyContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.example.bbmovie.security.websocket.WebSocketDecoratorConfig.AUTH_ATTRIBUTE;

@Log4j2
@Component
public class WebsocketJoseHandshakeInterceptor implements HandshakeInterceptor {

    private final JoseProviderStrategy joseProviderStrategy;

    @Autowired
    public WebsocketJoseHandshakeInterceptor(JoseProviderStrategyContext joseProviderStrategyContext) {
        this.joseProviderStrategy = joseProviderStrategyContext.getActiveProvider();
    }

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        log.info("[HandshakeInterceptor beforeHandshake]");
        String token = extractToken(request);
        if (token == null) {
            log.warn("No token found in request");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            if (!joseProviderStrategy.validateToken(token)) {
                log.error("Failed to validate token");
                return false;
            }

            log.info("[WebsocketJoseHandshakeInterceptor] validating token]");
            String username = joseProviderStrategy.getUsernameFromToken(token);
            List<String> roles = joseProviderStrategy.getRolesFromToken(token);
            log.info("Token validated. Username: {}, Roles: {}", username, roles);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, roles.stream().map(SimpleGrantedAuthority::new).toList()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails((HttpServletRequest) request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            attributes.put(AUTH_ATTRIBUTE, auth);
            return true;
        } catch (Exception e) {
            log.error("WebSocket JWT authentication failed: {}", e.getMessage(), e);

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                log.info("Bearer token found in request with Bearer token");
                return header.substring(7);
            }

            String token = header.replace(" ", "");
            log.info("Bearer token found in request");
            return token;
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
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.error("Handshake error: {}", exception.getMessage(), exception);
        }
    }
}
