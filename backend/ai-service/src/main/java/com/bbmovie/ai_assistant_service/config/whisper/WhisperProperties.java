package com.bbmovie.ai_assistant_service.config.whisper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Whisper service.
 * Allows customizing the behavior of the Whisper model loading.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "whisper")
public class WhisperProperties {
    private String modelPath = "classpath:models/ggml-tiny-q5_1.bin";
    private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    public int maxQueueSize = workerThreads * 5;
}
