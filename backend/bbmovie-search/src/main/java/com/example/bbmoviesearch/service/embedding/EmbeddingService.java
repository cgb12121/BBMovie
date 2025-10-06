package com.example.bbmoviesearch.service.embedding;

import reactor.core.publisher.Mono;

interface EmbeddingService {
    Mono<float[]> generateEmbedding(String text);
}