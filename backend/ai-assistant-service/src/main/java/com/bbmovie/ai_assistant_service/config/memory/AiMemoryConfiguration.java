package com.bbmovie.ai_assistant_service.config.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiMemoryConfiguration {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) { // Inject the bean
        return memoryId -> MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore) // Use the persistent store
                .id(memoryId)
                .build();
    }
}
