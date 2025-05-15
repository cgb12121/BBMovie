package com.example.bbmovie.security.oauth2.strategy.user.info;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2UserInfoStrategyFactory {

    private final Map<String, OAuth2UserInfoStrategy> strategies = new HashMap<>();

    public OAuth2UserInfoStrategyFactory(List<OAuth2UserInfoStrategy> strategyList) {
        for (OAuth2UserInfoStrategy strategy : strategyList) {
            strategies.put(strategy.getAuthProvider().name().toLowerCase(), strategy);
        }
    }

    public OAuth2UserInfoStrategy getStrategy(String provider) {
        return Optional.ofNullable(strategies.get(provider.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + provider));
    }
}
