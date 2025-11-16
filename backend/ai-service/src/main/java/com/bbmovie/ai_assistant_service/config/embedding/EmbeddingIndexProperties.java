package com.bbmovie.ai_assistant_service.config.embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.embedding.index")
public class EmbeddingIndexProperties {
    private String movies;
    private String rag;
}
