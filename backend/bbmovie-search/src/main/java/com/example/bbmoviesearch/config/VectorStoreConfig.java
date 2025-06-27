package com.example.bbmoviesearch.config;

import com.example.bbmoviesearch.service.embedding.LocalEmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingModel embeddingModel(LocalEmbeddingService localEmbeddingService) {
        return new EmbeddingModel() {
            @NonNull
            @Override
            public EmbeddingResponse call(@NonNull EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                List<Embedding> embeddings = texts.stream()
                        .map(text -> new Embedding(localEmbeddingService.generateEmbedding(text), text.hashCode()))
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @NonNull
            @Override
            public float[] embed(@NonNull String text) {
                return localEmbeddingService.generateEmbedding(text);
            }

            @NonNull
            @Override
            public float[] embed(@NonNull Document document) {
                return localEmbeddingService.generateEmbedding(document.getFormattedContent());
            }

            @NonNull
            @Override
            public List<float[]> embed(@NonNull List<String> texts) {
                return texts.stream()
                        .map(localEmbeddingService::generateEmbedding)
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
                        .map(document -> localEmbeddingService.generateEmbedding(document.getFormattedContent()))
                        .toList();
            }

            @Override
            public int dimensions() {
                return 384;
            }
        };
    }
}