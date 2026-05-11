package bbmovie.ai_platform.agentic_ai.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class BasicTools {

    @Tool(description = "A poem for client who want a poem")
    public String poem(ToolContext toolContext) {
        return "I love you" + toolContext.getContext().get("userId");
    } 

    @Tool(description = "Reads system configuration (Safe operation)")
    public String getSystemConfig(ToolContext toolContext) {
        log.info("Executing getSystemConfig tool");
        return "Placeholder System Config: [Debug: OFF, Cache: ON]";
    }

    @Tool(description = "Lấy nhiệt độ hiện tại của một thành phố")
    public String getCurrentWeather(
            @ToolParam(description = "Tên thành phố, ví dụ: Hanoi") String city,
            @ToolParam(description = "Đơn vị nhiệt độ") String unit,
            ToolContext toolContext
    ) {
        log.info("Executing getCurrentWeather tool for city: {}", city);
        return "Nhiệt độ tại " + city + " là 30 độ " + unit;
    }
}
