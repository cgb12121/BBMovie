package com.bbmovie.ai_assistant_service.core.low_level._config._embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _EmbeddingConfig {

    private final _EmbeddingSelector embeddingSelector;

    @Autowired
    public _EmbeddingConfig(_EmbeddingSelector embeddingSelector) {
        this.embeddingSelector = embeddingSelector;
    }

    @Value("${langchain4j.ollama.url}")
    private String ollama_url;

    @Bean("_EmbeddingModel")
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollama_url)
                .modelName(embeddingSelector.getModelName())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
