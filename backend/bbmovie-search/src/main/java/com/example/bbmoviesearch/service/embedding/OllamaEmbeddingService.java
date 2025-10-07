package com.example.bbmoviesearch.service.embedding;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
        return Mono.fromCallable(() -> embeddingModel.embed(text));
    }
}
