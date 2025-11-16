package com.bbmovie.ai_assistant_service.config.mapper;

import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JsonConfig {

    @Primary
    @Bean("ObjectMapper")
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(RagMovieDto.class, IgnoreEmbeddingMixin.class)
                .addMixIn(ToolExecutionRequest.class, ToolExecutionRequestMixin.class)
                .build();
    }
}
