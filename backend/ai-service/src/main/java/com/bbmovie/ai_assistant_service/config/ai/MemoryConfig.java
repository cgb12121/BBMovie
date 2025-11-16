package com.bbmovie.ai_assistant_service.config.ai;

import com.bbmovie.ai_assistant_service.config.cache.RedisProperties;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    @Bean("RedisMemoryStore")
    public ChatMemoryStore redisMemoryStore(RedisProperties properties) {
        return RedisChatMemoryStore.builder()
                .prefix("ai-assistant-service:chat-memory:")
                .host(properties.getHost())
                .port(properties.getPort())
                .password(properties.getPassword())
                .ttl((long) (60 * 30)) // second = > 30 minutes
                .build();
    }


    @Bean("ChatMemoryProvider")
    public ChatMemoryProvider chatMemoryProvider(@Qualifier("RedisMemoryStore") ChatMemoryStore store) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();
    }
}
