package com.bbmovie.ai_assistant_service.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component("AdminTools")
public class AdminTools {
    @Tool(name = "Admin System Information", value = "This is a very sensitive information that can be access by admin")
    public String adminAgentInformation() {
        return  "The admin system information is: Hello World with a lot of love";
    }
}
