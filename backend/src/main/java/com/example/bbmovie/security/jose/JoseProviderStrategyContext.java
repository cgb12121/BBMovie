package com.example.bbmovie.security.jose;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class JoseProviderStrategyContext {

    private final Map<String, JoseProviderStrategy> providers;

    @Getter
    @Setter
    private JoseProviderStrategy activeProvider;

    @Getter
    @Setter
    private JoseProviderStrategy previousProvider;

    public JoseProviderStrategyContext(
            Map<String, JoseProviderStrategy> providers,
            @Value("${app.jose.strategy}") String strategy
    ) {
        this.providers = providers;
        this.activeProvider = providers.get(strategy);
        this.previousProvider = this.activeProvider;
    }

    public Map<String, JoseProviderStrategy> getAll() {
        return Collections.unmodifiableMap(providers);
    }

    public synchronized void changeProvider(String strategy) {
        JoseProviderStrategy newProvider = providers.get(strategy);
        if (newProvider == null) {
            log.error("Requested strategy '{}' is not registered", strategy);
            throw new IllegalArgumentException("Invalid strategy: " + strategy);
        }

        this.previousProvider = this.activeProvider;
        this.activeProvider = newProvider;

        log.info("JOSE strategy changed to '{}'", strategy);
    }

    /**
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public boolean validateTokenWithFallback(String token) {
        if (activeProvider.validateToken(token)) return true;
        if (previousProvider != null && previousProvider.validateToken(token)) {
            log.warn("Token validated with previous provider — consider rotating tokens");
            return true;
        }
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public String getUsernameFromTokenWithFallback(String token) {
        try {
            return activeProvider.getUsernameFromToken(token);
        } catch (Exception e) {
            if (previousProvider != null) {
                try {
                    return previousProvider.getUsernameFromToken(token);
                } catch (Exception ignored) {
                    log.warn(e.getMessage());
                }
            }
            throw new IllegalArgumentException("Token username resolution failed");
        }
    }

    /**
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public List<String> getRolesFromTokenWithFallback(String token) {
        try {
            return activeProvider.getRolesFromToken(token);
        } catch (Exception e) {
            if (previousProvider != null) {
                try {
                    return previousProvider.getRolesFromToken(token);
                } catch (Exception ignored) {
                    log.warn("Token username resolution failed");
                }
            }
            throw new IllegalArgumentException("Token roles resolution failed");
        }
    }
}
