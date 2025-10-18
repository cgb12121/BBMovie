package com.bbmovie.gateway.security.anonymity;

import com.bbmovie.gateway.util.IpAddressUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Log4j2
@Order(-1)
public class IpAnonymityWebFilter implements WebFilter {

    private final AnonymityCheckService anonymityCheckService;

    @Autowired
    public IpAnonymityWebFilter(AnonymityCheckService anonymityCheckService) {
        this.anonymityCheckService = anonymityCheckService;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull WebFilterChain chain) {
        String ip = IpAddressUtils.getClientIp(exchange.getRequest());

        if (ip == null || ip.isEmpty()) {
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
}
