package com.bbmovie.ai_assistant_service._experimental._config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Experimental config for real-time streaming from Ollama.
 * Controlled by property: ai.experimental.streaming.enabled=true
 */
@Configuration
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _LlmMemoryConfig {

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
}
