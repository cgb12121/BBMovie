package com.example.bbmoviesearch.config;

import com.example.bbmoviesearch.service.embedding.DjLEmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;

@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingModel embeddingModel(DjLEmbeddingService djLEmbeddingService) {
        return new EmbeddingModel() {
            @NonNull
            @Override
            public EmbeddingResponse call(@NonNull EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                List<Embedding> embeddings = texts.stream()
                        .map(text -> {
                            float[] vec = djLEmbeddingService
                                    .generateEmbedding(text)
                                    .block();
                            return new Embedding(Objects.requireNonNull(vec), text.hashCode());
                        })
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @NonNull
            @Override
            public float[] embed(@NonNull String text) {
                return Objects.requireNonNull(djLEmbeddingService.generateEmbedding(text).block());
            }

            @NonNull
            @Override
            public float[] embed(@NonNull Document document) {
                return Objects.requireNonNull(djLEmbeddingService.generateEmbedding(document.getFormattedContent()).block());
            }

            @NonNull
            @Override
            public List<float[]> embed(@NonNull List<String> texts) {
                return texts.stream()
                        .map(t -> djLEmbeddingService.generateEmbedding(t).block())
                        .toList();
            }

            @NonNull
            @Override
            public List<float[]> embed(
                    @NonNull List<Document> documents,
                    @NonNull EmbeddingOptions options,
                    @NonNull BatchingStrategy batchingStrategy
            ) {
                return documents.stream()
                        .map(d -> djLEmbeddingService.generateEmbedding(d.getFormattedContent()).block())
                        .toList();
            }

            @Override
            public int dimensions() {
                return 384;
            }
        };
    }
}
