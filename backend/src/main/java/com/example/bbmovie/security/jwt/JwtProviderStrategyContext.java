package com.example.bbmovie.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JwtProviderStrategyContext {

    private final Map<String, JwtProviderStrategy> providers;

    private final JwtProviderStrategy activeProvider;

    public JwtProviderStrategyContext(
            Map<String, JwtProviderStrategy> providers,
            @Value("${app.jwt.strategy}") String strategy
    ) {
        this.providers = providers;
        this.activeProvider = providers.get(strategy);
    }

    public JwtProviderStrategy get() {
        return activeProvider;
    }

    public Map<String, JwtProviderStrategy> getAll() {
        return providers;
    }
}
