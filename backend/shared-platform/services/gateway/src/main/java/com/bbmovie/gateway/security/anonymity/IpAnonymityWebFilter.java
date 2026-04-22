package com.bbmovie.gateway.security.anonymity;

import com.bbmovie.gateway.config.ApplicationFilterOrder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Log4j2
@ConditionalOnProperty(
        value = "ip.filter.enabled",
        havingValue = "true"
)
public class IpAnonymityWebFilter implements GlobalFilter, Ordered {

    private final AnonymityCheckService anonymityCheckService;

    @Autowired
    public IpAnonymityWebFilter(AnonymityCheckService anonymityCheckService) {
        this.anonymityCheckService = anonymityCheckService;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull GatewayFilterChain chain) {
        String ip = getClientIp(exchange.getRequest());

        if (ip.isEmpty() || ip.equalsIgnoreCase("ANONYMOUS")) {
            return chain.filter(exchange); // No IP, let it pass
        }

        return anonymityCheckService.isAnonymous(ip)
                .flatMap(isAnonymous -> {
                    if (isAnonymous) {
                        log.warn("Blocking request from anonymous IP: {}", ip);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        return Optional.ofNullable(request.getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("anonymous");
    }

    @Override
    public int getOrder() {
        return ApplicationFilterOrder.ANONYMITY_CHECK_FILTER;
    }
}
