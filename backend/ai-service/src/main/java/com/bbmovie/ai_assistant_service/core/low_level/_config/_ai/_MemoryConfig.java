package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import com.bbmovie.ai_assistant_service.core.low_level._config._cache._RedisProperties;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Experimental config for real-time streaming from Ollama.
 * Controlled by property: ai.experimental.streaming.enabled=true
 */
@Configuration
public class _MemoryConfig {

    @Bean("_RedisMemoryStore")
    public ChatMemoryStore _RedisMemoryStore(_RedisProperties properties) {
        return RedisChatMemoryStore.builder()
                .prefix("ai-assistant-service:chat-memory:")
                .host(properties.getHost())
                .port(properties.getPort())
                .password(properties.getPassword())
                .ttl((long) (60 * 30)) // second = > 30 minutes
                .build();
    }


    @Bean("_ChatMemoryProvider")
    public ChatMemoryProvider _ChatMemoryProvider(
            @Qualifier("_RedisMemoryStore") ChatMemoryStore store) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();
    }
}
