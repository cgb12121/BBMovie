package com.bbmovie.search.service.embedding;

import com.bbmovie.search.exception.EmbeddingException;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;

@Log4j2
@Service
@ConditionalOnProperty(
        prefix = "embedding.provider.ollama",
        name = "enabled",
        havingValue = "true"
)
public class OllamaEmbeddingService implements  EmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Autowired
    public OllamaEmbeddingService(EmbeddingModel embeddingModel) {
        log.info("OllamaEmbeddingService is initialized");
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Mono<float[]> generateEmbedding(String text) {
        return Mono.fromCallable(() -> embeddingModel.embed(text))
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                                .filter(this::isTransientError) // Only retry on connection errors
                                .onRetryExhaustedThrow((spec, signal) -> {
                                    log.error("Ollama service is unavailable after multiple retries: {}, {}", spec, signal);
                                    return new EmbeddingException("Ollama service is unavailable after multiple retries.");
                                })
                );
    }

    private boolean isTransientError(Throwable throwable) {
        return throwable instanceof WebClientRequestException && throwable.getCause() instanceof ConnectException;
    }
}
