package bbmovie.ai_platform.agentic_ai.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BasicTools {

    private static final Logger log = LoggerFactory.getLogger(BasicTools.class);

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
