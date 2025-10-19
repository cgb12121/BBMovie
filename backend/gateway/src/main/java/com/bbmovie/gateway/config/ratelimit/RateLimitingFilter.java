package com.bbmovie.gateway.config.ratelimit;

import com.bbmovie.gateway.config.FilterOrder;
import com.example.common.entity.JoseConstraint;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final SubscriptionPlanResolver planResolver;
    private final Bucket4jConfigProperties config;
    private final List<CompiledFilterConfig> compiledFilters;
    private final AsyncProxyManager<byte[]> proxyManager;

    @Autowired
    public RateLimitingFilter(SubscriptionPlanResolver planResolver, Bucket4jConfigProperties config, AsyncProxyManager<byte[]> proxyManager) {
        this.planResolver = planResolver;
        this.config = config;
        this.proxyManager = proxyManager;

        // Compile all URL patterns
        this.compiledFilters = config.getFilters().stream()
                .map(filter -> new CompiledFilterConfig(
                        Pattern.compile(filter.getUrl()),
                        filter
                ))
                .collect(Collectors.toList());
    }

    private BucketConfiguration createBucketConfiguration(String plan, Bucket4jConfigProperties.FilterConfig filterConfig) {
        // Find matching rate limit config for the plan
        Bucket4jConfigProperties.RateLimitConfig rateLimitConfig = filterConfig.getRateLimits().stream()
                .filter(rl -> plan.equals(rl.getPlan()))
                .findFirst()
                .orElse(null);

        if (rateLimitConfig == null) {
            log.warn("No rate limit config found for plan: {}, using default ANONYMOUS limits", plan);
            // Fallback to ANONYMOUS if plan not found
            rateLimitConfig = filterConfig.getRateLimits().stream()
                    .filter(rl -> "ANONYMOUS".equals(rl.getPlan()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No ANONYMOUS fallback rate limit config found"));
        }

        ConfigurationBuilder builder = BucketConfiguration.builder();

        // Add all bandwidths from config
        for (Bucket4jConfigProperties.BandwidthConfig bandwidth : rateLimitConfig.getBandwidths()) {
            Duration duration = parseDuration(bandwidth.getTime(), bandwidth.getUnit());
            builder.addLimit(Bandwidth.classic(
                    bandwidth.getCapacity(),
                    Refill.intervally(bandwidth.getCapacity(), duration)
            ));
        }

        return builder.build();
    }

    private Duration parseDuration(long time, String unit) {
        return switch (unit.toLowerCase()) {
            case "seconds" -> Duration.ofSeconds(time);
            case "minutes" -> Duration.ofMinutes(time);
            case "hours" -> Duration.ofHours(time);
            case "days" -> Duration.ofDays(time);
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check if rate limiting is enabled
        if (!config.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        // Find a matching filter config for this URL
        CompiledFilterConfig matchedFilter = compiledFilters.stream()
                .filter(cf -> cf.pattern().matcher(path).matches())
                .findFirst()
                .orElse(null);

        // If no filter matches, allow the request
        if (matchedFilter == null) {
            log.debug("No rate limit filter matches path: {}", path);
            return chain.filter(exchange);
        }

        Map<String, Object> claims = planResolver.getClaims(exchange.getRequest());
        String plan = resolvePlan(claims);
        String userIdentifier = resolveKey(claims, exchange.getRequest());

        // Include URL pattern in a cache key to separate limits per endpoint
        String urlIdentifier = matchedFilter.config().getUrl().replaceAll("[^a-zA-Z0-9]", "_");
        String cacheKey = "rate-limit:" + urlIdentifier + ":" + plan + ":" + userIdentifier;

        log.debug("Rate limiting - Path: {}, Plan: {}, Key: {}", path, plan, userIdentifier);

        // Get or create a bucket in Redis reactively
        return Mono.fromCompletionStage(() -> {
                    byte[] cacheKeyBytes = cacheKey.getBytes(StandardCharsets.UTF_8);
                    return proxyManager.builder()
                            .build(cacheKeyBytes, () -> CompletableFuture.completedFuture(createBucketConfiguration(plan, matchedFilter.config())))
                            .tryConsumeAndReturnRemaining(1);
                })
                .flatMap(probe -> {
                    if (probe.isConsumed()) {
                        exchange.getResponse()
                                .getHeaders()
                                .add("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                        return chain.filter(exchange);
                    } else {
                        return rateLimitExceededResponse(exchange, probe);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Rate limiting error for key {}: {}", cacheKey, e.getMessage());
                    // Fail open: allow request if Redis operation fails
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange, ConsumptionProbe probe) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(config.getDefaultHttpStatusCode()));
        exchange.getResponse()
                .getHeaders()
                .add("X-Rate-Limit-Remaining",
                        String.valueOf(probe.getRemainingTokens())
                );
        exchange.getResponse()
                .getHeaders()
                .add("X-Rate-Limit-Retry-After-Seconds",
                        String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000)
                );

        // Set the content type from config
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.parseMediaType(config.getDefaultHttpContentType()));

        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(
                config.getDefaultHttpResponseBody().getBytes(StandardCharsets.UTF_8)
        );
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolvePlan(Map<String, Object> claims) {
        return (String) claims.getOrDefault(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER, "ANONYMOUS");
    }

    private String resolveKey(Map<String, Object> claims, ServerHttpRequest request) {
        return (String) claims.getOrDefault(JoseConstraint.JosePayload.SID,
                Optional.ofNullable(request.getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .orElse("anonymous-fallback"));
    }

    @Override
    public int getOrder() {
        return FilterOrder.SECOND;
    }
}
