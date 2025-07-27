package com.bbmovie.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Component
public class AuthHeaderFilter extends AbstractGatewayFilterFactory<AuthHeaderFilter.Config> {

    public AuthHeaderFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("Authorization", token)
                .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    @Getter
    @Setter
    public static class Config {
        private boolean enabled;
    }
}