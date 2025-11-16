package com.bbmovie.ai_assistant_service.config.tool;

import com.bbmovie.ai_assistant_service.tool.type.shared.RagTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolAliasConfig {

    private final RagTool ragTool;

    @Autowired
    public ToolAliasConfig(@Qualifier("ragTool") RagTool ragTool) {
        this.ragTool = ragTool;
    }

    @Bean("adminRagTool")
    @Qualifier("adminTools")
    public RagTool adminRagTool() {
        return ragTool;
    }

    @Bean("modRagTool")
    @Qualifier("modTools")
    public RagTool modRagTool() {
        return ragTool;
    }

    @Bean("userRagTool")
    @Qualifier("userTools")
    public RagTool userRagTool() {
        return ragTool;
    }
}
