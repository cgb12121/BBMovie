package bbmovie.ai_platform.agentic_ai.config;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resilience4j configuration — creates one {@link CircuitBreaker} per AI provider
 * and a shared {@link Retry} policy for transient errors.
 *
 * <p>Circuit breaker instances are configured in {@code application.properties} under:
 * {@code resilience4j.circuitbreaker.instances.<provider>.*}
 *
 * <p>Retry policy is configured under:
 * {@code resilience4j.retry.instances.ai-provider.*}
 *
 * <p>The bean names match the provider identifiers in {@link bbmovie.ai_platform.agentic_ai.entity.enums.AiModel}:
 * {@code "ollama"}, {@code "google"}, {@code "groq"}.
 */
@Configuration
public class AiResilienceConfig {

    /**
     * Provides a map of circuit breakers keyed by AI provider name.
     *
     * <p>Derived dynamically from {@link AiModel} — every distinct {@code provider} value
     * in the enum gets its own {@link CircuitBreaker} instance, configured via
     * {@code application.properties} under
     * {@code resilience4j.circuitbreaker.instances.<provider>.*}.
     * If no explicit config exists for a provider, Resilience4j uses its default config.
     *
     * <p>Adding a new {@link AiModel} with a new provider automatically creates the CB
     * here — no manual update of this class required.
     */
    @Bean
    public Map<String, CircuitBreaker> providerCircuitBreakers(CircuitBreakerRegistry registry) {
        return Arrays.stream(AiModel.values())
                .map(AiModel::getProvider)
                .distinct()
                .collect(Collectors.toMap(
                        provider -> provider,
                        registry::circuitBreaker  // Looks up config by name, falls back to default
                ));
    }

    /**
     * Shared retry policy for all AI provider calls.
     * Configured to retry only on transient errors (429, 503) — see
     * {@link bbmovie.ai_platform.agentic_ai.service.chat.ResilientChatService#isTransientError}.
     */
    @Bean
    public Retry aiProviderRetry(RetryRegistry registry) {
        return registry.retry("ai-provider");
    }
}
