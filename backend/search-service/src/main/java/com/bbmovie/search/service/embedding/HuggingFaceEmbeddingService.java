package com.bbmovie.search.service.embedding;

import com.bbmovie.search.exception.EmbeddingException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * @deprecated
 *
 *  Fallback when the local model is not being used
 */
@Service
@Log4j2
@Deprecated(since = "1.0.0", forRemoval = true)
@ConditionalOnMissingBean({
        DjLEmbeddingService.class,
        OllamaEmbeddingService.class
})
public class HuggingFaceEmbeddingService implements EmbeddingService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(5);

    private final WebClient webClient;

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Autowired
    public HuggingFaceEmbeddingService(WebClient huggingFaceWebClient) {
        this.webClient = huggingFaceWebClient;
    }

    @Override
    public Mono<float[]> generateEmbedding(String text) {
        return webClient.post()
                //The api might be changed, get the laster from hugging face if
                .uri("https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/sentence-similarity")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(Collections.singletonList(text))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("HuggingFace API returned an error: {}", response.statusCode());
                    if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new EmbeddingException("HuggingFace API returned a bad request."));
                    }
                    if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.error(new EmbeddingException("HuggingFace API returned an unauthorized request."));
                    }
                    if (response.statusCode() == HttpStatus.FORBIDDEN) {
                        return Mono.error(new EmbeddingException("HuggingFace API returned a forbidden request."));
                    }
                    return Mono.error(new EmbeddingException(response.statusCode().toString()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(new EmbeddingException("HuggingFace service unavailable."))
                )
                .bodyToMono(List.class)
                .retryWhen(
                        Retry.backoff(MAX_ATTEMPTS, INITIAL_BACKOFF)
                                .filter(EmbeddingException.class::isInstance)
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new EmbeddingException("HuggingFace API failed after retries.")
                                )
                )
                .map(this::parseEmbedding)
                .doOnError(e -> log.error("Failed to generate embedding from HuggingFace", e));
    }

    private float[] parseEmbedding(List<?> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            throw new EmbeddingException("Empty response from HuggingFace API.");
        }
        List<?> vectorList = (List<?>) responseBody.getFirst();
        float[] embedding = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            embedding[i] = ((Number) vectorList.get(i)).floatValue();
        }
        return embedding;
    }
}
