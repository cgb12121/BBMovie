package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Resilience decorator for {@link ChatService}.
 *
 * <p>Applies circuit breaker and retry policies around the core {@link ChatServiceImpl}
 * delegate. By using the Decorator pattern with {@code @Primary}, all components that
 * inject {@link ChatService} automatically receive the resilient version — no changes
 * needed elsewhere.
 *
 * <p><b>Operator order:</b>
 * <pre>
 *   CircuitBreaker (outer) → Retry (inner)
 * </pre>
 * CB is outermost so that when the circuit is open, retries do not execute against a
 * known-dead provider. Only transient errors (429, 503) are retried.
 *
 * <p><b>Circuit breaker selection:</b> One CB instance per provider ({@code "ollama"},
 * {@code "google"}, {@code "groq"}), configured in {@code application.properties} under
 * {@code resilience4j.circuitbreaker.instances.<provider>.*}.
 * Falls back to the {@code "ollama"} CB if the provider is unknown.
 *
 * <p><b>SSE timeout sentinel:</b> {@link ChatServiceImpl#STREAM_TIMEOUT_SENTINEL} emitted
 * by the inner stream is NOT treated as an error — it passes through the CB/retry unchanged.
 */
@Slf4j
@Primary
@Service
public class ResilientChatService implements ChatService {

    private final ChatService delegate;
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final Retry retry;

    public ResilientChatService(
            @Qualifier("chatServiceImpl") ChatService delegate,
            Map<String, CircuitBreaker> providerCircuitBreakers,
            Retry aiProviderRetry) {
        this.delegate = delegate;
        this.circuitBreakers = providerCircuitBreakers;
        this.retry = aiProviderRetry;
    }

    @Override
    public Flux<String> chat(
        UUID sessionId, UUID userId, String message,UUID parentId, UUID assetId, 
        AiMode mode, AiModel model, String userRole) {
        CircuitBreaker cb = resolveCircuitBreaker(model);

        return delegate.chat(sessionId, userId, message, parentId, assetId, mode, model, userRole)
                .transformDeferred(CircuitBreakerOperator.of(cb))   // Outer: CB — blocks calls when open
                .retryWhen(
                    reactor.util.retry.Retry.backoff(retry.getRetryConfig().getMaxAttempts(),
                    Duration.ofMillis(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                )
                .filter(this::isTransientError)
                .doBeforeRetry(sig -> log.warn("[Resilience] Retrying AI call (attempt {}): {}", sig.totalRetries() + 1, sig.failure().getMessage())))
                .onErrorResume(CallNotPermittedException.class, ex -> {
                    // Circuit is open — provider is considered down. Emit a clear sentinel.
                    log.warn("[Resilience] Circuit OPEN for provider '{}'. Returning degraded response.",
                            model != null ? model.getProvider() : "unknown");
                    return Flux.just("[PROVIDER_UNAVAILABLE]");
                });
    }

    @Override
    public Flux<String> regenerateMessage(UUID messageId, UUID userId) {
        // regenerate and edit don't need per-provider CB since model is resolved internally,
        // but we still apply the default CB for baseline protection.
        CircuitBreaker defaultCb = circuitBreakers.getOrDefault("ollama", circuitBreakers.values().iterator().next());

        return delegate.regenerateMessage(messageId, userId)
                .transformDeferred(CircuitBreakerOperator.of(defaultCb))
                .retryWhen(buildRetrySpec())
                .onErrorResume(CallNotPermittedException.class, ex -> Flux.just("[PROVIDER_UNAVAILABLE]"));
    }

    @Override
    public Flux<String> editMessage(UUID messageId, UUID userId, String newContent) {
        CircuitBreaker defaultCb = circuitBreakers.getOrDefault("ollama", circuitBreakers.values().iterator().next());

        return delegate.editMessage(messageId, userId, newContent)
                .transformDeferred(CircuitBreakerOperator.of(defaultCb))
                .retryWhen(buildRetrySpec())
                .onErrorResume(CallNotPermittedException.class, ex -> Flux.just("[PROVIDER_UNAVAILABLE]"));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Selects the circuit breaker for the given model's provider.
     * Falls back to "ollama" CB if model is null or provider is not registered.
     */
    private CircuitBreaker resolveCircuitBreaker(AiModel model) {
        if (model == null) {
            return circuitBreakers.getOrDefault("ollama", circuitBreakers.values().iterator().next());
        }
        return circuitBreakers.getOrDefault(model.getProvider(),
                circuitBreakers.getOrDefault("ollama", circuitBreakers.values().iterator().next()));
    }

    /**
     * Returns true for errors that are safe to retry:
     * <ul>
     *   <li>HTTP 429 — provider rate-limited us; backoff and retry</li>
     *   <li>HTTP 503 — provider temporarily unavailable</li>
     * </ul>
     * All other errors (model errors, malformed request, etc.) are NOT retried.
     */
    private boolean isTransientError(Throwable t) {
        if (t instanceof WebClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            return status == HttpStatus.TOO_MANY_REQUESTS
                    || status == HttpStatus.SERVICE_UNAVAILABLE;
        }
        return false;
    }

    private reactor.util.retry.Retry buildRetrySpec() {
        return reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                .filter(this::isTransientError)
                .doBeforeRetry(sig -> log.warn("[Resilience] Retrying (attempt {}): {}",
                        sig.totalRetries() + 1, sig.failure().getMessage())
                );
    }
}
