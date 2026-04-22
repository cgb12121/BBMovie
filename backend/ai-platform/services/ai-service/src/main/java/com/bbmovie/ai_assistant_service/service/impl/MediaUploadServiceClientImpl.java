package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.service.MediaUploadServiceClient;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementation of MediaUploadServiceClient that calls media-upload-service
 * to get presigned download URLs for files.
 */
@Service
@RequiredArgsConstructor
public class MediaUploadServiceClientImpl implements MediaUploadServiceClient {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(MediaUploadServiceClientImpl.class);

    @Qualifier("mediaUploadServiceWebClient")
    private final WebClient mediaUploadServiceWebClient;

    @Override
    public Mono<String> getDownloadUrl(String uploadId, String jwtToken) {
        log.debug("Getting download URL for uploadId: {}", uploadId);
        
        return mediaUploadServiceWebClient.get()
                .uri("/upload/files/{uploadId}/url", uploadId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .<String>handle((response, sink) -> {
                    if (response.has("downloadUrl")) {
                        sink.next(response.get("downloadUrl").asText());
                        return;
                    }
                    sink.error(new RuntimeException("Invalid response from media-upload-service: missing downloadUrl"));
                })
                .doOnError(e -> log.error("Failed to get download URL for uploadId {}: {}", uploadId, e.getMessage()))
                .onErrorMap(e -> new RuntimeException("Failed to get file download URL: " + e.getMessage(), e));
    }
}

