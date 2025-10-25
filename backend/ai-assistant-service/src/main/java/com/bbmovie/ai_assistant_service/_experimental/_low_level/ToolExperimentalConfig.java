package com.bbmovie.ai_assistant_service._experimental._low_level;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolExperimentalConfig {

    private final ToolExperimental toolInstance;

    public ToolExperimentalConfig(ToolExperimental toolInstance) {
        this.toolInstance = toolInstance;
    }

    @Bean
    public List<ToolSpecification> toolSpecifications() {
        return ToolSpecifications.toolSpecificationsFrom(toolInstance);
    }

    @Bean
    public ToolExecutor toolExecutor() throws NoSuchMethodException {
        return new DefaultToolExecutor(toolInstance, toolInstance.getClass().getMethod("aLovePoem", String.class));
    }
}