package com.example.bbmoviesearch.service.embedding;

import reactor.core.publisher.Mono;

public interface EmbeddingService {
    Mono<float[]> generateEmbedding(String text);
}