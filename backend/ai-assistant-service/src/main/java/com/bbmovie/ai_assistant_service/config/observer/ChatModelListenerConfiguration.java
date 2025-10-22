package com.bbmovie.ai_assistant_service.config.observer;

import dev.langchain4j.model.chat.listener.ChatModelListener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelListenerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "logging.chat", name = "enabled", havingValue = "true")
    public ChatModelListener chatModelListener() {
        return new CustomChatModelListener();
    }
}