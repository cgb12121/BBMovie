package com.bbmovie.ai_assistant_service.core.low_level._config._tool;

import com.bbmovie.ai_assistant_service.core.low_level._tool._type._shared._RagTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _ToolAliasConfig {

    private final _RagTool ragTool;

    @Autowired
    public _ToolAliasConfig(@Qualifier("_RagTool") _RagTool ragTool) {
        this.ragTool = ragTool;
    }

    @Bean("_AdminRagTool")
    @Qualifier("_AdminTools")
    public _RagTool adminRagTool() {
        return ragTool;
    }

    @Bean("_ModRagTool")
    @Qualifier("_ModTools")
    public _RagTool modRagTool() {
        return ragTool;
    }

    @Bean("_UserRagTool")
    @Qualifier("_UserTools")
    public _RagTool userRagTool() {
        return ragTool;
    }
}
