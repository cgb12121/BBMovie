package com.bbmovie.ai_assistant_service.core.low_level._config._mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _JsonConfig {

    @Bean("_ObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(ToolExecutionRequest.class, _ToolExecutionRequestMixin.class);
        return objectMapper;
    }

}
