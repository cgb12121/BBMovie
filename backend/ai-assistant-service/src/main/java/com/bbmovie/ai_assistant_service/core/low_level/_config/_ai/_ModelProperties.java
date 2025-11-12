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

    // ========== AI Model Properties ==========
    private Double temperature = 0.7;
    private Integer topK = 40;
    private Double topP = 0.9;
    private Double minP = 0.05;
    private Integer numCtx = 32768;
    private Integer numPredict = 1024;
    private Integer seed = 2004;
}
