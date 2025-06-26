package com.example.bbmovie.config;

import com.example.bbmovie.service.embedding.LocalEmbeddingService;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingModel embeddingModel(LocalEmbeddingService localEmbeddingService) {
        return new EmbeddingModel() {
            @NotNull
            @Override
            public EmbeddingResponse call(@NotNull EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                List<Embedding> embeddings = texts.stream()
                        .map(text -> new Embedding(localEmbeddingService.generateEmbedding(text), text.hashCode()))
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @NotNull
            @Override
            public float[] embed(@NotNull String text) {
                return localEmbeddingService.generateEmbedding(text);
            }

            @NotNull
            @Override
            public float[] embed(@NotNull Document document) {
                return localEmbeddingService.generateEmbedding(document.getFormattedContent());
            }

            @NotNull
            @Override
            public List<float[]> embed(@NotNull List<String> texts) {
                return texts.stream()
                        .map(localEmbeddingService::generateEmbedding)
                        .toList();
            }

            @NotNull
            @Override
            public List<float[]> embed(
                    @NotNull List<Document> documents,
                    @NotNull EmbeddingOptions options,
                    @NotNull BatchingStrategy batchingStrategy
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