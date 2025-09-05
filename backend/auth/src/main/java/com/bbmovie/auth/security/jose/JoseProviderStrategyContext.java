package com.bbmovie.auth.security.jose;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static com.example.common.entity.JoseConstraint.JwtType.JWE;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

/**
 * A context class used to manage and interact with different implementations of
 * {@link JoseProviderStrategy}. This class allows the system to dynamically switch
 * between different JOSE (JSON Object Signing and Encryption) strategy providers at runtime.
 * <p>
 * Responsibilities:
 * - Maintains a collection of registered {@link JoseProviderStrategy} implementations.
 * - Keeps track of the currently active JOSE strategy provider.
 * - Allows switching between different JOSE strategies during runtime.
 * - Provides access to an unmodifiable view of all registered strategy providers.
 * <p>
 * Features:
 * - Dynamically switching strategies ensure flexibility in adapting to various runtime
 *   requirements, such as key rotations or differing authentication mechanisms.
 * <p>
 * Usage of this class ensures flexibility and centralized control over the selection and
 * application of JOSE strategies in the system.
 */
@Log4j2
@Component
public class JoseProviderStrategyContext {

    private final Map<String, JoseProviderStrategy> providers;

    @Getter
    private JoseProviderStrategy activeProvider;

    @Getter
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

    public JoseProviderStrategy getLastActiveJwe() {
        if (activeProvider != null && JWE.equals(activeProvider.getType())) {
            return activeProvider;
        }
        if (previousProvider != null && JWE.equals(previousProvider.getType())) {
            return previousProvider;
        }
        return null;
    }

    public JoseProviderStrategy getLastActiveJws() {
        if (activeProvider != null && JWS.equals(previousProvider.getType())) {
            return activeProvider;
        }
        if (previousProvider != null && JWS.equals(previousProvider.getType())) {
            return previousProvider;
        }
        return null;
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
}