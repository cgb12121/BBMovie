package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.ai_common.dto.IngestedContentDto;
import com.bbmovie.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRoutingService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Fetches refined content from ai-assets.
     * Retries if the content is still being processed by the underlying engines (Java/Rust).
     */
    public Mono<String> getRefinedContent(UUID assetId) {
        log.info("[ContentRouting] Fetching content for asset: {}", assetId);

        return webClientBuilder.build()
                .get()
                .uri("http://ai-assets/api/v1/assets/{id}/content", assetId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND, response -> 
                    Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "INGESTION_PENDING")))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<IngestedContentDto>>() {})
                .flatMap(response -> {
                    if (response.getData() != null) {
                        return Mono.just(response.getData().getContent());
                    }
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "INGESTION_PENDING"));
                })
                .retryWhen(Retry.backoff(15, Duration.ofMillis(800))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(e -> e instanceof ResponseStatusException && 
                                ((ResponseStatusException) e).getReason().equals("INGESTION_PENDING"))
                        .doBeforeRetry(s -> log.info("[ContentRouting] Still waiting for ingestion of asset {}. Attempt: {}", assetId, s.totalRetries() + 1))
                )
                .onErrorResume(e -> {
                    log.error("[ContentRouting] Failed to fetch content for {}: {}", assetId, e.getMessage());
                    return Mono.just("Warning: File context is currently unavailable.");
                });
    }
}
