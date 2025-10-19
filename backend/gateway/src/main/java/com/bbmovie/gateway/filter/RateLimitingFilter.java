package com.bbmovie.gateway.filter;

import com.bbmovie.gateway.config.ratelimit.SubscriptionPlanResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final SubscriptionPlanResolver planResolver;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingFilter(SubscriptionPlanResolver planResolver) {
        this.planResolver = planResolver;
    }

    private Bucket createNewBucket(String plan) {
        return switch (plan) {
            case "FREE" -> Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofHours(1))))
                    .addLimit(Bandwidth.classic(20, Refill.intervally(30, Duration.ofMinutes(1))))
                    .build();
            case "PREMIUM" -> Bucket.builder()
                    .addLimit(Bandwidth.classic(500, Refill.intervally(5000, Duration.ofHours(1))))
                    .addLimit(Bandwidth.classic(100, Refill.intervally(200, Duration.ofMinutes(1))))
                    .build();
            default -> // ANONYMOUS or any other case
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(10, Refill.intervally(5, Duration.ofMinutes(1))))
                            .build();
        };
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Filtering request: {}", exchange.getRequest().getURI());
        String plan = planResolver.resolvePlan(exchange.getRequest());
        String key = planResolver.resolveKey(exchange.getRequest());

        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket(plan));

        // Using Mono.fromCallable to avoid blocking the event loop
        return Mono.fromCallable(() -> bucket.tryConsumeAndReturnRemaining(1))
                .flatMap(probe -> {
                    if (probe.isConsumed()) {
                        exchange.getResponse()
                                .getHeaders()
                                .add("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse()
                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse()
                                .getHeaders()
                                .add("X-Rate-Limit-Retry-After-Seconds",
                                        String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000)
                                );
                        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
                        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                        DataBuffer buffer = bufferFactory.wrap("Too many request".getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
