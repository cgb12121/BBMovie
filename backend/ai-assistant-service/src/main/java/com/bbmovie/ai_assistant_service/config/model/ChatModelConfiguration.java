package com.bbmovie.ai_assistant_service.config.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ChatModelConfiguration {

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        // This provider will create a new MessageWindowChatMemory for each unique memoryId
        // with a maximum of 10 messages.
        return memoryId -> MessageWindowChatMemory.builder()
                .maxMessages(10)
                // .chatMemoryStore(chatMemoryStore) // Uncomment and inject if you have a persistent store
                // .id(memoryId) // The id for ChatMemory will be managed by the provider, but can be set if needed
                .build();
    }

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new ChatMemoryStore() {
            private final Map<Object, List<ChatMessage>> store = new ConcurrentHashMap<>();

            @Override
            public List<ChatMessage> getMessages(Object memoryId) {
                return store.getOrDefault(memoryId, Collections.emptyList());
            }

            @Override
            public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                store.put(memoryId, new ArrayList<>(messages));
            }

            @Override
            public void deleteMessages(Object memoryId) {
                store.remove(memoryId);
            }
        };
    }
}
