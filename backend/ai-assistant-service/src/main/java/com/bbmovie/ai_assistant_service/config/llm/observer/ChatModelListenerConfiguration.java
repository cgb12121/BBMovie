package com.bbmovie.ai_assistant_service.config.llm.observer;

import dev.langchain4j.model.chat.listener.ChatModelListener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelListenerConfiguration {

    @Bean
    @ConditionalOnBooleanProperty(name = "logging.chat.enabled")
    public ChatModelListener chatModelListener() {
        return new CustomChatModelListener();
    }
}