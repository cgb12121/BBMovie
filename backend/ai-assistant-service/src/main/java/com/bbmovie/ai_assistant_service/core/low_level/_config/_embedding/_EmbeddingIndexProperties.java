package com.bbmovie.ai_assistant_service.core.low_level._config._embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.embedding.index")
public class _EmbeddingIndexProperties {
    private String movies;
    private String rag;
}
