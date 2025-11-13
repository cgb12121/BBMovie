package com.bbmovie.ai_assistant_service.core.low_level._config._embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.embedding")
public class _EmbeddingProperties {
    private String modelName;
    private String index;
    private int dimension;
    private String embeddingField;
}
