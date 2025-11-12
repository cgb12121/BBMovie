package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai")
public class _ModelProperties {

    /**
     * The ID of the active AI model to use, e.g. "QWEN3", "LLAMA3", etc.
     */
    private String activeModel = "QWEN3"; // default fallback

    /**
     * Whether to include persona prompts.
     */
    private boolean enablePersona = true;
}
