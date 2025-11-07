package com.bbmovie.ai_assistant_service.core.low_level._config._cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.redis")
public class _RedisProperties {
    private String host;
    private int port;
    private String password;
    private int database;
}
