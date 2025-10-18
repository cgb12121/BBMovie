package com.bbmovie.gateway.security;

import com.bbmovie.gateway.config.FilterOrder;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.http.HttpStatus;
import reactor.core.scheduler.Schedulers;

import static com.example.common.entity.JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.SID;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

@Log4j2
@Component
public class JwtBlacklistFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Boolean> reactiveRedis;
    private final WebClient authWebClient;
    private final WebClient apiKeyWebClient;
    private final ObjectMapper objectMapper;
    private final String abacEndpoint;

    @Value("${gateway.config.security.api-key-header}")
    private String apiKeyHeader;

    @Value("${internal.url.auth.api-key}")
    private String apiKey;

    @Autowired
    public JwtBlacklistFilter(
            ReactiveRedisTemplate<String, Boolean> reactiveRedis,
            @Qualifier("authWebClient") @LoadBalanced WebClient authWebClient,
            @Qualifier("apiKeyWebClient") WebClient apiKeyWebClient,
            ObjectMapper objectMapper,
            @Value("${internal.url.auth.abac-endpoint}") String abacEndpoint
    ) {
        this.reactiveRedis = reactiveRedis;
        this.authWebClient = authWebClient;
        this.apiKeyWebClient = apiKeyWebClient;
        this.objectMapper = objectMapper;
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
        String clientApiKey = exchange.getRequest()
                .getHeaders()
                .getFirst(apiKeyHeader);

        if (token == null && apiKey == null) {
            log.warn("No token or API key provided for path: {}", exchange.getRequest().getPath());
            return setUnauthorizedResponse(exchange, "No token or API key provided");
        }

        if (apiKey != null) {
            return validateApiKey(clientApiKey, exchange, chain);
        }

        return validateJwtToken(token, exchange, chain);
    }

    private Mono<Void> validateApiKey(String apiKey, ServerWebExchange exchange, GatewayFilterChain chain) {
        return apiKeyWebClient.get()
                .header(apiKeyHeader, apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(status -> {
                    if (!"valid".equals(status)) {
                        log.warn("Invalid API key: {}", apiKey);
                        return setUnauthorizedResponse(exchange, "Invalid API key");
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(err -> {
                    log.warn("API key validation failed: {}", err.getMessage());
                    return setUnauthorizedResponse(exchange, "Invalid API key");
                });
    }

    private Mono<Void> validateJwtToken(String token, ServerWebExchange exchange, GatewayFilterChain chain) {
        return parseJwtPayload(token)
                .flatMap(payload -> {
                    String sid = payload.get(SID).asText();
                    String username = payload.get(SUB).asText();
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
                                    return reactiveRedis
                                            .delete(abacKey)
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
        if (!StringUtils.hasText(token)) {
            return Mono.error(new IllegalArgumentException("Token must not be null or empty"));
        }

        JwtType type = JwtType.getType(token);
        if (type == null) {
            return Mono.error(new IllegalArgumentException("Unable to determine JWT type"));
        }

        String rawPayload = JwtType.getPayload(token);
        if (!StringUtils.hasText(rawPayload)) {
            return Mono.error(new IllegalArgumentException("Payload must not be null or empty"));
        }

        if (JWS.equals(type)) {
            return Mono.fromCallable(() -> {
                        byte[] decoded = Base64.getUrlDecoder().decode(rawPayload);
                        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorMap(e -> new RuntimeException("Failed to parse JWS payload", e));
        }

        return Mono.error(new IllegalArgumentException("Unsupported JWT type: " + type));
    }

    private Mono<String> fetchNewToken(String oldAccessToken) {
        Map<String, String> requestBody = Map.of("oldAccessToken", oldAccessToken);
        log.info("Fetching access token with new abac from auth service");
        return authWebClient
                .post()
                .uri(abacEndpoint)
                .header(apiKeyHeader, apiKey)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(err -> log.error("WebClient error fetching new token: {}", err.getMessage()));
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

    @Override
    public int getOrder() {
        return FilterOrder.SECOND;
    }
}
