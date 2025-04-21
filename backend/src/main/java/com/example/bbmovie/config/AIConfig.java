package com.example.bbmovie.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new TransformersEmbeddingModel(MetadataMode.EMBED);
    }
}
