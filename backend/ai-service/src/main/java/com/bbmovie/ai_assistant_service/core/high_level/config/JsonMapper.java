package com.bbmovie.ai_assistant_service.core.high_level.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapper {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
