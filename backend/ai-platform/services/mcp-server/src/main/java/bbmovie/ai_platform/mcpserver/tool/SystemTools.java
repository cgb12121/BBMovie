package bbmovie.ai_platform.mcpserver.tool;

import bbmovie.ai_platform.aop_policy.annotation.Monitored;
import bbmovie.ai_platform.aop_policy.hitl.RequiresApproval;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

@Configuration
public class SystemTools {

    private static final Logger log = LoggerFactory.getLogger(SystemTools.class);

    public record BashCommandRequest(String command) {}
    public record BashCommandResponse(String output) {}

    @Bean
    @Description("Executes a raw Bash or PowerShell command on the host system. This is a highly privileged tool.")
    @Monitored
    @RequiresApproval(action = "SYSTEM_COMMAND", description = "Executes a shell command on the host server")
    public Function<BashCommandRequest, BashCommandResponse> executeBashCommand() {
        return request -> {
            log.info("Executing bash command: {}", request.command());
            // Mock execution for safety
            return new BashCommandResponse("Mock execution of command: " + request.command());
        };
    }

    public record WeatherRequest(String location) {}
    public record WeatherResponse(String weather) {}

    @Bean
    @Description("Get the current weather for a specific location.")
    @Monitored
    public Function<WeatherRequest, WeatherResponse> getWeather() {
        return request -> {
            log.info("Fetching weather for: {}", request.location());
            return new WeatherResponse("Sunny, 25C in " + request.location());
        };
    }
}
