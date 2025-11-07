package com.bbmovie.ai_assistant_service.core.low_level._config._mapper;

import com.bbmovie.ai_assistant_service.core.low_level._dto._RagMovieDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _JsonConfig {

    @Bean("_ObjectMapper")
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(_RagMovieDto.class, _IgnoreEmbeddingMixin.class)
                .addMixIn(ToolExecutionRequest.class, _ToolExecutionRequestMixin.class)
                .build();
    }
}
