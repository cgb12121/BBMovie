package com.bbmovie.ai_assistant_service.config.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    private final EmbeddingSelector embeddingSelector;

    @Autowired
    public EmbeddingConfig(EmbeddingSelector embeddingSelector) {
        this.embeddingSelector = embeddingSelector;
    }

    @Value("${ai.ollama.url}")
    private String ollamaUrl;

    @Bean("_EmbeddingModel")
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(embeddingSelector.getModelName())
                .logRequests(false)
                .logResponses(false) // Should not log massive embeddings (384 dimensions means a float array of 384)
                .build();
    }
}
