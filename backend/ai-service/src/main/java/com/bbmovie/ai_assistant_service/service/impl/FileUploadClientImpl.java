package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.service.FileUploadClient;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FileUploadClientImpl implements FileUploadClient {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(FileUploadClientImpl.class);

    @Qualifier("fileServiceWebClient")
    private final WebClient fileServiceWebClient;

    @Override
    public Mono<Void> confirmFile(Long fileId) {
        log.info("Confirming usage of file: {}", fileId);
        return fileServiceWebClient.put()
                .uri("/internal/files/{id}/confirm", fileId)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnError(e -> log.error("Failed to confirm file {}: {}", fileId, e.getMessage()));
    }
}