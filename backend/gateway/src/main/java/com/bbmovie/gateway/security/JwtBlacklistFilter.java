package com.bbmovie.gateway.security;

import com.bbmovie.gateway.config.ApplicationFilterOrder;
import com.bbmovie.gateway.exception.InvalidAuthenticationMethodException;
import com.example.common.entity.JoseConstraint.JwtType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static com.example.common.entity.JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.SID;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

@Log4j2
@Component
public class JwtBlacklistFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Boolean> reactiveRedis;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtBlacklistFilter(ReactiveRedisTemplate<String, Boolean> reactiveRedis, ObjectMapper objectMapper) {
        this.reactiveRedis = reactiveRedis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return createResponse(exchange, "Page not found", HttpStatus.NOT_FOUND);
        }

        boolean isPublicRoute = Optional.ofNullable(route.getMetadata().get("public"))
                .map(Object::toString)
                .map("true"::equalsIgnoreCase)
                .orElse(false);
        String path = exchange.getRequest()
                .getPath()
                .toString();

        if (isPublicRoute || path.equals("/.well-known/jwks.json")) {
            return chain.filter(exchange);
        }

        return isTokenBlacklisted(exchange, chain);
    }

    private Mono<Void> isTokenBlacklisted(ServerWebExchange exchange, GatewayFilterChain chain) {
        String bearerToken = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        String token = (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
        return parseJwtPayload(token)
                .flatMap(payload -> {
                    String sid = payload.get(SID).asText();
                    return checkBlacklists(sid, exchange, chain);
                })
                .onErrorResume(ex -> {
                    log.error("Error processing token: {}", ex.getMessage());
                    return createResponse(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> checkBlacklists(String sid, ServerWebExchange exchange, GatewayFilterChain chain) {
        String logoutKey = JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        String abacKey = JWT_ABAC_BLACKLIST_PREFIX + sid;

        Mono<Boolean> logoutMono = reactiveRedis.opsForValue().get(logoutKey).defaultIfEmpty(false);
        Mono<Boolean> abacMono = reactiveRedis.opsForValue().get(abacKey).defaultIfEmpty(false);

        return Mono.zip(logoutMono, abacMono)
                .flatMap(tuple -> {
                    boolean logout = tuple.getT1();
                    boolean abac = tuple.getT2();

                    if (logout) {
                        log.info("Token has been logged out: {}", sid);
                        return createResponse(exchange, "Session has been logged out", HttpStatus.UNAUTHORIZED);
                    } else if (abac) {
                        log.info("Token has been revoked: {}", sid);
                        return createResponse(exchange, "Stale token", HttpStatus.UNAUTHORIZED);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<JsonNode> parseJwtPayload(String token) {
        if (!StringUtils.hasText(token)) {
            return Mono.error(new InvalidAuthenticationMethodException("No valid authentication method(s) found."));
        }

        JwtType type = JwtType.getType(token);
        if (type == null) {
            return Mono.error(new InvalidAuthenticationMethodException("Unable to determine JWT type"));
        }

        String rawPayload = JwtType.getPayload(token);
        if (!StringUtils.hasText(rawPayload)) {
            return Mono.error(new InvalidAuthenticationMethodException("Payload must not be null or empty"));
        }

        if (JWS.equals(type)) {
            return Mono.fromCallable(() -> {
                        byte[] decoded = Base64.getUrlDecoder().decode(rawPayload);
                        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorMap(e -> {
                        log.error("Failed to parse JWS payload: {}", e.getMessage());
                        return new InvalidAuthenticationMethodException("Failed to parse JWS payload");
                    });
        }

        return Mono.error(new InvalidAuthenticationMethodException("Unsupported JWT type: " + type));
    }

    private Mono<Void> createResponse(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse()
                .setStatusCode(status);
        exchange.getResponse()
                .getHeaders()
                .add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return ApplicationFilterOrder.AUTHENTICATION_FILTER;
    }
}