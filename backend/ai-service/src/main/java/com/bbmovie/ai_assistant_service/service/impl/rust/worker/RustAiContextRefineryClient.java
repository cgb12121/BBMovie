package com.bbmovie.ai_assistant_service.service.impl.rust.worker;

import com.bbmovie.ai_assistant_service.dto.response.RefineryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class RustAiContextRefineryClient {

    private final WebClient webClient;

    @Value("${rust.ai.service.enabled:true}")
    private boolean enabled;

    public RustAiContextRefineryClient(@Qualifier("rustWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Calls the Rust batch processing endpoint.
     * 
     * @param requests List of file processing requests
     * @return List of JsonNodes containing the results
     */
    public Mono<List<JsonNode>> processBatch(List<RustProcessRequest> requests) {
        if (!enabled) {
            log.warn("Rust service disabled, skipping batch processing.");
            return Mono.empty();
        }

        ProcessBatchRequest payload = new ProcessBatchRequest(requests);

        return webClient.post()
                .uri("/api/process-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RefineryResponse<List<JsonNode>>>() {})
                .<List<JsonNode>>handle((response, sink) -> {
                    if ((!response.isSuccess())) {
                        sink.error(new RuntimeException("Rust Service Batch Error: " + response.getErrorSummary()));
                        return;
                    }
                    sink.next(response.getData());
                })
                .onErrorResume(e -> {
                    log.error("Failed to call Rust Service Batch: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Failed to call Rust Service: " + e.getMessage()));
                });
    }

    @Data
    @AllArgsConstructor
    public static class RustProcessRequest {
        private String file_url;
        private String filename;
    }

    @Data
    @AllArgsConstructor
    private static class ProcessBatchRequest {
        private List<RustProcessRequest> requests;
    }
}