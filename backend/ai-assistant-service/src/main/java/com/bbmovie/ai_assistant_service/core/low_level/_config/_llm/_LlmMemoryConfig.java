package com.bbmovie.ai_assistant_service.core.low_level._config._llm;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Experimental config for real-time streaming from Ollama.
 * Controlled by property: ai.experimental.streaming.enabled=true
 */
@Configuration
public class _LlmMemoryConfig {

    @Bean("_StreamingChatMemoryStore")
    public ChatMemoryStore _MemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean("_ChatMemoryProvider")
    public ChatMemoryProvider _ChatMemoryProvider(
            @Qualifier("_StreamingChatMemoryStore") ChatMemoryStore store) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(50)
                .chatMemoryStore(store)
                .build();
    }
}
