package com.bbmovie.ai_assistant_service.config.tool;

import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.tool.AiTools;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.bbmovie.ai_assistant_service.entity.model.AssistantType.*;

/**
 * This class is responsible for creating the tool registry.
 * Anonymous tools are not registered as the anonymous users don't have access to tools.
 */
@Configuration
public class ToolRegistryConfig {

    @Bean
    @Qualifier("adminToolRegistry")
    public ToolsRegistry adminToolRegistry(
            @Qualifier("adminTools") List<AiTools> adminTools,
            @Qualifier("commonTools") List<AiTools> commonTools
    ) {
        return init(ADMIN, adminTools, commonTools);
    }

    @Bean
    @Qualifier("modToolRegistry")
    public ToolsRegistry modToolRegistry(
            @Qualifier("modTools") List<AiTools> modTools,
            @Qualifier("commonTools") List<AiTools> commonTools
    ) {
        return init(MOD, modTools, commonTools);
    }

    @Bean
    @Qualifier("userToolRegistry")
    public ToolsRegistry userToolRegistry(
            @Qualifier("userTools") List<AiTools> userTools,
            @Qualifier("commonTools") List<AiTools> commonTools
    ) {
        return init(USER, userTools, commonTools);
    }

    @SafeVarargs
    private ToolsRegistry init(AssistantType type, List<AiTools>... tools) {
        List<AiTools> mergedTools = new ArrayList<>();
        for (List<AiTools> tool : tools) {
            if (tool != null) mergedTools.addAll(tool);
        }
        return new ToolsRegistry(type, mergedTools);
    }
}