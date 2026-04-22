package com.bbmovie.ai_assistant_service.config.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.rag.elasticsearch")
public class ESProperties {
    private String host;
    private int port;
    private String scheme;
}
