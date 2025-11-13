package com.bbmovie.ai_assistant_service.global_config.ai_mode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Master config: activates only one AI integration mode (high or low)
 * depending on application.properties.
 */
@Configuration
public class AiModeConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "ai.mode", havingValue = "high-level", matchIfMissing = true)
    @ComponentScan(basePackages = "com.bbmovie.ai_assistant_service.core.high_level")
    static class HighLevelMode { }

    @Configuration
    @ConditionalOnProperty(name = "ai.mode", havingValue = "low-level")
    @ComponentScan(basePackages = "com.bbmovie.ai_assistant_service.core.low_level")
    static class LowLevelMode { }
}