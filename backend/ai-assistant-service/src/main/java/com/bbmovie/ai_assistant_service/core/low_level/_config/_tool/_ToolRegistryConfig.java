package com.bbmovie.ai_assistant_service.core.low_level._config._tool;

import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTools;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * This class is responsible for creating the tool registry.
 * Anonymous tools are not registered as the anonymous users don't have access to tools.
 */
@Configuration
public class _ToolRegistryConfig {

    @Bean
    @Qualifier("_AdminToolRegistry")
    public _ToolsRegistry adminToolRegistry(@Qualifier("_AdminTools") List<_AiTools> adminTools) {
        return new _ToolsRegistry(adminTools);
    }

    @Bean
    @Qualifier("_ModToolRegistry")
    public _ToolsRegistry modToolRegistry(@Qualifier("_ModTools") List<_AiTools> modTools) {
        return new _ToolsRegistry(modTools);
    }

    @Bean
    @Qualifier("_UserToolRegistry")
    public _ToolsRegistry customerToolRegistry(@Qualifier("_UserTools") List<_AiTools> userTools) {
        return new _ToolsRegistry(userTools);
    }
}