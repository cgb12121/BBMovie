package bbmovie.ai_platform.agentic_ai.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;

import java.util.List;
import java.util.stream.Collectors;

@CommandGroup
@Slf4j
@RequiredArgsConstructor
public class AgenticShell {

    private final ChatModel chatModel;
    private final ApplicationContext applicationContext;
    private final List<ToolCallback> toolCallbacks;

    // ANSI Color Codes for Premium UI
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";
    private static final String PURPLE = "\u001B[35m";
    private static final String WHITE = "\u001B[37m";

    @Command(name = "info", description = "Check model and LLM provider info")
    public String info() {
        String provider = chatModel.getClass().getSimpleName();
        String details = chatModel.toString();

        return "\n" + BOLD + PURPLE + "── AI SYSTEM STATUS ──" + RESET + "\n" +
               BOLD + WHITE + "  📡 Provider: " + RESET + GREEN + provider + RESET + "\n" +
               BOLD + WHITE + "  📝 Details:  " + RESET + WHITE + details + RESET + "\n" +
               BOLD + PURPLE + "──────────────────────" + RESET + "\n";
    }

    @Command(name = "tools", description = "List registered local tools")
    public String tools() {
        if (toolCallbacks.isEmpty()) {
            return YELLOW + "⚠️ No local tools registered." + RESET;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(BOLD).append(GREEN).append("🛠️  LOCAL AGENT TOOLS").append(RESET).append("\n");
        sb.append(WHITE).append("┌──────────────────────┬────────────────────────────────────────────────────┐").append(RESET).append("\n");
        sb.append(WHITE).append("│ ").append(BOLD).append(CYAN).append(String.format("%-20s", "Tool Name")).append(RESET).append(WHITE).append(" │ ").append(BOLD).append(CYAN).append(String.format("%-50s", "Description")).append(RESET).append(WHITE).append(" │").append(RESET).append("\n");
        sb.append(WHITE).append("├──────────────────────┼────────────────────────────────────────────────────┤").append(RESET).append("\n");

        for (ToolCallback tc : toolCallbacks) {
            String name = tc.getToolDefinition().name();
            String desc = tc.getToolDefinition().description();
            if (desc.length() > 50) desc = desc.substring(0, 47) + "...";
            
            sb.append(WHITE).append("│ ").append(RESET).append(GREEN).append(String.format("%-20s", name)).append(RESET).append(WHITE).append(" │ ").append(RESET).append(WHITE).append(String.format("%-50s", desc)).append(RESET).append(WHITE).append(" │").append(RESET).append("\n");
        }
        
        sb.append(WHITE).append("└──────────────────────┴────────────────────────────────────────────────────┘").append(RESET).append("\n");
        return sb.toString();
    }

    @Command(name = "mcp", description = "Check MCP clients and tools/skills")
    public String mcp() {
        List<String> mcpBeanNames = List.of(applicationContext.getBeanDefinitionNames()).stream()
                .filter(name -> name.toLowerCase().contains("mcp"))
                .collect(Collectors.toList());

        if (mcpBeanNames.isEmpty()) {
            return YELLOW + "⚠️ No MCP related beans found in context." + RESET;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(BOLD).append(CYAN).append("🔌 MCP INFRASTRUCTURE").append(RESET).append("\n");
        sb.append(CYAN).append("────────────────────────────────────────────────────").append(RESET).append("\n");

        for (String name : mcpBeanNames) {
            Object bean = applicationContext.getBean(name);
            sb.append(BOLD).append("  🔹 ").append(WHITE).append(name).append(RESET).append("\n");
            sb.append("     ").append(BOLD).append(WHITE).append("Type: ").append(RESET).append(WHITE).append(bean.getClass().getSimpleName()).append(RESET).append("\n");
            
            try {
                if (bean.getClass().getSimpleName().contains("McpClient") || 
                    bean.getClass().getSimpleName().contains("McpSyncClient")) {
                    
                    java.lang.reflect.Method listToolsMethod = bean.getClass().getMethod("listTools");
                    Object toolsResponse = listToolsMethod.invoke(bean);
                    
                    if (toolsResponse != null) {
                        sb.append("     ").append(GREEN).append("● Active").append(RESET).append(" - MCP Tools discovered").append("\n");
                    }
                }
            } catch (Exception e) {
                // Skip
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
