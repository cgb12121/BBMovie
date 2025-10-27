package com.bbmovie.ai_assistant_service._experimental._low_level._config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Experimental config for real-time streaming from Ollama.
 * Controlled by property: ai.experimental.streaming.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "ai.experimental.streaming.enabled", havingValue = "true")
public class _StreamingConfig {

    @Bean("experimentalStreamingChatMemoryStore")
    public ChatMemoryStore experimentalMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean("experimentalChatMemoryProvider")
    public ChatMemoryProvider experimentalChatMemoryProvider(
            @Qualifier("experimentalStreamingChatMemoryStore") ChatMemoryStore store) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(50)
                .chatMemoryStore(store)
                .build();
    }

    @Bean("experimentalStreaming")
    public StreamingChatModel streamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3:0.6b-q4_K_M")
                .temperature(0.7)
                .listeners(List.of(new _ChatListener()))
                .build();
    }
}
