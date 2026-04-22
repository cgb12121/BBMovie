package com.bbmovie.transcodeworker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for JSON processing dependencies.
 * Provides an ObjectMapper bean for JSON serialization and deserialization operations.
 */
@Configuration
public class JsonConfig {

    /**
     * Creates and returns an ObjectMapper bean instance.
     *
     * @return ObjectMapper instance for JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
