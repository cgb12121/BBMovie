package com.bbmovie.ai_assistant_service.service;

import reactor.core.publisher.Mono;

public interface FileUploadClient {
    // New methods for confirming file usage (for file-service)
    Mono<Void> confirmFile(Long fileId);
}