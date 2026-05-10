package bbmovie.ai_platform.agentic_ai.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.function.Function;

@Configuration
public class BasicTools {

    private static final Logger log = LoggerFactory.getLogger(BasicTools.class);

    public record SystemConfigRequest() {}
    public record SystemConfigResponse(String result) {}

    @Bean
    @Description("Reads system configuration (Safe operation)")
    public Function<SystemConfigRequest, SystemConfigResponse> getSystemConfig() {
        return request -> {
            log.info("Executing getSystemConfig tool");
            return new SystemConfigResponse("Placeholder System Config: [Debug: OFF, Cache: ON]");
        };
    }
}
