package com.bbmovie.gateway.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AuthHeaderMutator extends AbstractGatewayFilterFactory<AuthHeaderMutator.Config> implements Ordered {

    public AuthHeaderMutator() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            String apiKey = exchange.getRequest()
                    .getHeaders()
                    .getFirst(config.getApiKeyHeader());

            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .build();

            if (apiKey != null) {
                request = request.mutate()
                        .header(config.getApiKeyHeader(), apiKey)
                        .build();
            }

            ServerWebExchange forward = exchange.mutate()
                    .request(request)
                    .build();

            return chain.filter(forward);
        };
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Getter
    @Setter
    @Component
    @ConfigurationProperties("gateway.config.security")
    public static class Config {
        private boolean enabled = true;
        private String apiKeyHeader = "X-Api-Key";
    }
}