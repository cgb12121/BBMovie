package com.bbmovie.gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpStatus;

@Log4j2
@Component
public class JwtBlacklistFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Boolean> redisTemplate;
    private final WebClient authWebClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtBlacklistFilter(
            ReactiveRedisTemplate<String, Boolean> redisTemplate,
            @LoadBalanced WebClient authWebClient,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.authWebClient = authWebClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractJwtFromRequest(exchange);
        if (token == null) {
            log.warn("No token provided in request");
            return setUnauthorizedResponse(exchange, "No token provided");
        }

        return parseJwtPayload(token)
                .flatMap(payloadJson -> {
                    String sid = payloadJson.get("sid").asText();
                    String username = payloadJson.get("sub").asText();

                    String logoutKey = "logout-blacklist:" + sid;
                    String abacKey = "abac-blacklist:" + sid;

                    return redisTemplate.opsForValue().get(logoutKey)
                            .flatMap(isLogoutBlacklisted -> {
                                if (Boolean.TRUE.equals(isLogoutBlacklisted)) {
                                    log.warn("Token with sid {} is logout-blacklisted", sid);
                                    return setUnauthorizedResponse(exchange, "Session has been logged out");
                                }
                                return redisTemplate.opsForValue().get(abacKey)
                                        .flatMap(isAbacBlacklisted -> {
                                            if (Boolean.TRUE.equals(isAbacBlacklisted)) {
                                                log.info("Token with sid {} is ABAC-blacklisted, fetching new tokens for user: {}", sid, username);
                                                return fetchNewToken(username)
                                                        .flatMap(newToken -> {
                                                            exchange.getResponse().getHeaders().add("Authorization", newToken);
                                                            return redisTemplate.delete(abacKey)
                                                                    .then(chain.filter(exchange));
                                                        })
                                                        .onErrorResume(err -> {
                                                            log.error("Failed to fetch new token for user: {}", username, err);
                                                            return setUnauthorizedResponse(exchange, "Unable to update ABAC attributes");
                                                        });
                                            }
                                            return chain.filter(exchange);
                                        });
                            });
                })
                .onErrorResume(ex -> {
                    log.error("Error processing token: {}", ex.getMessage());
                    return setUnauthorizedResponse(exchange, "Invalid token");
                });
    }

    private Mono<JsonNode> parseJwtPayload(String token) {
        return Mono.fromCallable(() -> {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readTree(payload);
        });
    }

    private Mono<String> fetchNewToken(String username) {
        return authWebClient
                .post()
                .uri("/auth/abac/new-access-token/{username}", username)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String extractJwtFromRequest(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }

    private Mono<Void> setUnauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain");
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
