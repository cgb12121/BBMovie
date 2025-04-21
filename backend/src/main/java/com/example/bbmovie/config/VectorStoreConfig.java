package com.example.bbmovie.config;

import com.example.bbmovie.service.impl.HuggingFaceService;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingModel embeddingModel(HuggingFaceService huggingFaceService) {
        return new EmbeddingModel() {
            @NotNull
            @Override
            public EmbeddingResponse call(@NotNull EmbeddingRequest request) {
                List<String> texts = request.getInstructions();
                List<Embedding> embeddings = texts.stream()
                        .map(text -> new Embedding(huggingFaceService.generateEmbedding(text), text.hashCode()))
                        .collect(Collectors.toList());
                return new EmbeddingResponse(embeddings);

            }

            @NotNull
            @Override
            public float[] embed(@NotNull String text) {
                return huggingFaceService.generateEmbedding(text);
            }

            @NotNull
            @Override
            public float[] embed(@NotNull Document document) {
                return huggingFaceService.generateEmbedding(document.getFormattedContent());
            }

            @NotNull
            @Override
            public List<float[]> embed(@NotNull List<String> texts) {
                return texts.stream()
                        .map(huggingFaceService::generateEmbedding)
                        .collect(Collectors.toList());
            }

            @NotNull
            @Override
            public List<float[]> embed(
                    @NotNull List<Document> documents,
                    @NotNull EmbeddingOptions options,
                    @NotNull BatchingStrategy batchingStrategy
            ) {
                return documents.stream()
                        .map(document -> huggingFaceService.generateEmbedding(document.getFormattedContent()))
                        .collect(Collectors.toList());
            }

            @Override
            public int dimensions() {
                return 384;
            }
        };
    }
}