package com.bbmovie.gateway.security;

import com.bbmovie.gateway.config.FilterOrderConfig;
import com.bbmovie.gateway.exception.IpAddressException;
import com.example.common.entity.JoseConstraint.JwtType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.http.HttpStatus;

import static com.bbmovie.gateway.config.RateLimiterConfig.DEFAULT_RATE_LIMITER_KEY;
import static com.example.common.entity.JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.SID;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;
import static com.example.common.entity.JoseConstraint.JwtType.JWE;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

@Log4j2
@Component
public class JwtBlacklistFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Boolean> reactiveRedis;
    private final WebClient authWebClient;
    private final WebClient apiKeyWebClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String abacEndpoint;

    @Autowired
    public JwtBlacklistFilter(
            ReactiveRedisTemplate<String, Boolean> reactiveRedis,
            @Qualifier("authWebClient") @LoadBalanced WebClient authWebClient,
            @Qualifier("apiKeyWebClient") WebClient apiKeyWebClient,
            ObjectMapper objectMapper,
            @Value("${internal.url.auth.api-key}") String apiKey,
            @Value("${internal.url.auth.abac-endpoint}") String abacEndpoint
    ) {
        this.reactiveRedis = reactiveRedis;
        this.authWebClient = authWebClient;
        this.apiKeyWebClient = apiKeyWebClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.abacEndpoint = abacEndpoint;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            log.error("No route found for request");
            return setNotFoundResponse(exchange);
        }

        boolean isPublicRoute = isPublicRoute(route);
        String path = exchange.getRequest()
                .getPath()
                .toString();
        log.info("Processing route: {}, isPublic: {}", path, isPublicRoute);

        if (isPublicRoute || path.equals("/.well-known/jwks.json")) {
            exchange.getAttributes()
                    .put(DEFAULT_RATE_LIMITER_KEY, getClientIp(exchange));
            log.debug("Skipping validation for public route: {}", path);
            return chain.filter(exchange);
        }

        return processAuthentication(exchange, chain);
    }

    private boolean isPublicRoute(Route route) {
        return Optional.ofNullable(
                    route.getMetadata().get("public")
                )
                .map(Object::toString)
                .map("true"::equals)
                .orElse(false);
    }

    private Mono<Void> processAuthentication(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractJwtFromRequest(exchange);
        String apiKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Api-Key");

        if (token == null && apiKey == null) {
            log.warn("No token or API key provided for path: {}", exchange.getRequest().getPath());
            exchange.getAttributes()
                    .put(DEFAULT_RATE_LIMITER_KEY, getClientIp(exchange));
            return setUnauthorizedResponse(exchange, "No token or API key provided");
        }

        if (apiKey != null) {
            return validateApiKey(apiKey, exchange, chain);
        }

        return validateJwtToken(token, exchange, chain);
    }

    private Mono<Void> validateApiKey(String apiKey, ServerWebExchange exchange, GatewayFilterChain chain) {
        return apiKeyWebClient.get()
                .header("X-Api-Key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(status -> {
                    if (!"valid".equals(status)) {
                        log.warn("Invalid API key: {}", apiKey);
                        exchange.getAttributes()
                                .put(DEFAULT_RATE_LIMITER_KEY, getClientIp(exchange));
                        return setUnauthorizedResponse(exchange, "Invalid API key");
                    }
                    exchange.getAttributes()
                            .put(DEFAULT_RATE_LIMITER_KEY, "api-key:" + apiKey);
                    return chain.filter(exchange);
                })
                .onErrorResume(err -> {
                    log.warn("API key validation failed: {}", err.getMessage());
                    exchange.getAttributes()
                            .put(DEFAULT_RATE_LIMITER_KEY, getClientIp(exchange));
                    return setUnauthorizedResponse(exchange, "Invalid API key");
                });
    }

    private Mono<Void> validateJwtToken(String token, ServerWebExchange exchange, GatewayFilterChain chain) {
        return parseJwtPayload(token)
                .flatMap(payload -> {
                    String sid = payload.get(SID).asText();
                    String username = payload.get(SUB).asText();
                    exchange.getAttributes().put(DEFAULT_RATE_LIMITER_KEY, "sid:" + sid);
                    return checkBlacklists(sid, username, exchange, chain);
                })
                .onErrorResume(ex -> handleTokenError(ex, exchange));
    }

    private Mono<Void> checkBlacklists(String sid, String username, ServerWebExchange exchange, GatewayFilterChain chain) {
        String logoutKey = JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        String abacKey = JWT_ABAC_BLACKLIST_PREFIX + sid;

        return reactiveRedis.opsForValue().get(logoutKey)
                .flatMap(isLogoutBlacklisted -> {
                    if (Boolean.TRUE.equals(isLogoutBlacklisted)) {
                        log.warn("Token with sid {} is logout-blacklisted", sid);
                        return setUnauthorizedResponse(exchange, "Session has been logged out");
                    }
                    return checkAbacBlacklist(abacKey, sid, username, exchange, chain);
                });
    }

    private Mono<Void> checkAbacBlacklist(
            String abacKey, String sid, String username,
            ServerWebExchange exchange, GatewayFilterChain chain
    ) {
        String jwtToken = extractJwtFromRequest(exchange);
        return reactiveRedis.opsForValue().get(abacKey)
                .flatMap(isAbacBlacklisted -> {
                    if (Boolean.TRUE.equals(isAbacBlacklisted)) {
                        log.info("Token with sid {} is ABAC-blacklisted, fetching new token for user: {}", sid, username);
                        return fetchNewToken(jwtToken)
                                .flatMap(newToken -> {
                                    exchange.getResponse()
                                            .getHeaders()
                                            .set(HttpHeaders.AUTHORIZATION, newToken);
                                    return reactiveRedis.delete(abacKey)
                                            .then(chain.filter(exchange));
                                })
                                .onErrorResume(err -> {
                                    log.error("Failed to fetch new token for user: {}", username, err);
                                    return setUnauthorizedResponse(exchange, "Unable to update ABAC attributes");
                                });
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> handleTokenError(Throwable ex, ServerWebExchange exchange) {
        if (ex instanceof IpAddressException) {
            log.warn("Invalid IP address: {}", ex.getMessage());
            return setUnauthorizedResponse(exchange, "Detected proxy/vpn, please turn off.");
        }
        log.error("Error processing token: {}", ex.getMessage());
        return setUnauthorizedResponse(exchange, "Invalid token");
    }

    private String extractJwtFromRequest(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }

    private Mono<JsonNode> parseJwtPayload(String token) {
        JwtType type = JwtType.getType(token);
        String rawPayload = JwtType.getPayload(token);

        if (JWS.equals(type)) {
            return Mono.fromCallable(() -> {
                byte[] decoded = Base64.getUrlDecoder().decode(rawPayload);
                return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            });
        }

        if (JWE.equals(type)) {
            return decodeJwePayload(token)
                    .handle((jwePayload, sink) -> {
                        try {
                            sink.next(objectMapper.readTree(jwePayload));
                        } catch (IOException e) {
                            sink.error(new RuntimeException("Failed to parse JWE payload", e));
                        }
                    });
        }

        return Mono.error(new IllegalArgumentException("Invalid JWT token type: " + type));
    }

    private Mono<String> fetchNewToken(String oldAccessToken) {
        Map<String, String> requestBody = Map.of("oldAccessToken", oldAccessToken);
        log.info("Fetching access token with new abac from auth service");
        return authWebClient
                .post()
                .uri(abacEndpoint)
                .header("X-API-KEY", apiKey)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(err -> log.error("WebClient error fetching new token: {}", err.getMessage()));
    }

    private Mono<String> decodeJwePayload(String jweToken) {
        Map<String, String> requestBody = Map.of("token", jweToken);
        return authWebClient
                .post()
                .uri("/jwe/payload")
                .header("X-API-KEY", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(err -> log.error("WebClient error decoding JWE: {}", err.getMessage()));
    }

    private Mono<Void> setUnauthorizedResponse(ServerWebExchange exchange, String message) {
        return createResponse(exchange, message, HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> setNotFoundResponse(ServerWebExchange exchange) {
        return createResponse(exchange, "No route found", HttpStatus.NOT_FOUND);
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

    private String getClientIp(ServerWebExchange exchange) {
        try {
            return Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                    .getAddress()
                    .getHostAddress();
        } catch (Exception e) {
            throw new IpAddressException("Cannot get client ip: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return FilterOrderConfig.SECOND;
    }
}
