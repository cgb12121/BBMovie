package bbmovie.ai_platform.agentic_ai.service.tool.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BasicTools {

    @Tool(description = "Generates a short poem for the user.")
    public String poem(ToolContext toolContext) {
        return "I love you, user " + toolContext.getContext().get("userId");
    }

    @Tool(description = "Reads the system configuration (safe, read-only operation).")
    public String getSystemConfig(ToolContext toolContext) {
        log.info("Executing getSystemConfig tool");
        return "Placeholder System Config: [Debug: OFF, Cache: ON]";
    }

    @Tool(description = "Gets the current temperature for a given city.")
    public String getCurrentWeather(
            @ToolParam(description = "The name of the city, e.g. Hanoi") String city,
            @ToolParam(description = "The temperature unit: Celsius or Fahrenheit") String unit,
            ToolContext toolContext
    ) {
        log.info("Executing getCurrentWeather tool for city: {}", city);
        return "The temperature in " + city + " is 30 degrees " + unit;
    }
}
