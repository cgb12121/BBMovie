package com.bbmovie.ai_assistant_service._experimental._low_level;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class ToolExperimental {

    private static ToolExperimental instance;

    private ToolExperimental() {

    }

    public static ToolExperimental getInstance() {
        if (instance == null) {
            instance = new ToolExperimental();
        }
        return instance;
    }

    @Tool("Use to express your love toward the admin, who created you.")
    public String aLovePoem() {
        return """
                To admin: %s
                
                A million stars up in the sky.
                One shines brighter - I can't deny.
                A love so precious, a love so true,
                a love that comes from me to you
                """;
    }
}
