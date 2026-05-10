package bbmovie.ai_platform.agentic_ai;

import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = "bbmovie.ai_platform",
    exclude = {McpClientAutoConfiguration.class}
)
public class AgenticAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticAiApplication.class, args);
    }

}
