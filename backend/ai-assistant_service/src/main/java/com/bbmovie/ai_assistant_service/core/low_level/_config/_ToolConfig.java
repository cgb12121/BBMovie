package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTool;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class _ToolConfig {

    @Bean
    @Qualifier("_AdminToolRegistry")
    public _ToolRegistry adminToolRegistry(@Qualifier("_AdminTool") List<_AiTool> adminTools) {
        return new _ToolRegistry(adminTools);
    }
}
