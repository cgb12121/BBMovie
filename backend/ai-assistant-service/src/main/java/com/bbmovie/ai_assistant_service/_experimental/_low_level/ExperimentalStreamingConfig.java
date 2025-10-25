package com.bbmovie.ai_assistant_service._experimental._low_level;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Experimental config for real-time streaming from Ollama.
 * Controlled by property: ai.experimental.streaming.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "ai.experimental.streaming.enabled", havingValue = "true")
public class ExperimentalStreamingConfig {

    @Bean("experimentalStreamingChatMemory")
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(20);
    }

    @Bean("experimentalStreaming")
    public StreamingChatModel streamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434") // your Ollama endpoint
                .modelName("qwen3:0.6b-q4_K_M")             // adjust as needed
                .temperature(0.7)
                .build();
    }
}
