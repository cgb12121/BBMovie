package com.bbmovie.ai_assistant_service.config.tool;

import com.bbmovie.ai_assistant_service.tool.AiTools;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ToolsRegistry adminToolRegistry(@Qualifier("adminTools") List<AiTools> adminTools) {
        return new ToolsRegistry(ADMIN, adminTools);
    }

    @Bean
    @Qualifier("modToolRegistry")
    public ToolsRegistry modToolRegistry(@Qualifier("modTools") List<AiTools> modTools) {
        return new ToolsRegistry(MOD, modTools);
    }

    @Bean
    @Qualifier("userToolRegistry")
    public ToolsRegistry userToolRegistry(@Qualifier("userTools") List<AiTools> userTools) {
        return new ToolsRegistry(USER, userTools);
    }
}