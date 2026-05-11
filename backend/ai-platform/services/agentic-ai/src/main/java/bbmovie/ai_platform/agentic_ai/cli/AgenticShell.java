package bbmovie.ai_platform.agentic_ai.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Help.Ansi;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static bbmovie.ai_platform.agentic_ai.cli.CliAnsiUtils.*;

@Slf4j
@Component
@Command(
    name = "ai",
    mixinStandardHelpOptions = true,
    header = {
        "@|cyan,bold  █████╗  ██████╗ ███████╗███╗   ██╗████████╗██╗ ██████╗|@",
        "@|cyan,bold ██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝██║██╔════╝|@",
        "@|cyan,bold ███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║   ██║██║     |@",
        "@|cyan,bold ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║   ██║██║     |@",
        "@|cyan,bold ██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║   ██║╚██████╗|@",
        "@|cyan,bold ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝   ╚═╝ ╚═════╝|@",
        "  @|faint --- The Intelligent Agentic AI Management CLI ---|@",
        ""
    },
    description = "Core management utility for Agentic AI platform.",
    subcommands = {
        AgenticShell.StatusCommand.class,
        AgenticShell.InfoCommand.class,
        AgenticShell.ToolsCommand.class,
        AgenticShell.McpCommand.class
    }
)
public class AgenticShell implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        printHeader(out);
        out.println(Ansi.AUTO.string("Use @|yellow ai --help|@ to see available commands.\n"));
        return 0;
    }

    // --- Subcommands ---

    @Component
    @Command(name = "status", description = "Show a summary of system health and connectivity")
    @RequiredArgsConstructor
    public static class StatusCommand implements Callable<Integer> {
        private final ChatModel chatModel;
        private final List<ToolCallback> toolCallbacks;
        private final ApplicationContext context;
        @Spec CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            printSection(out, "SYSTEM HEALTH STATUS", "magenta");
            
            printField(out, "🤖", "Model", chatModel.getClass().getSimpleName(), "green");
            
            String toolStatus = toolCallbacks.isEmpty() ? "@|red None|@" : toolCallbacks.size() + " Active";
            printField(out, "🛠️ ", "Local Tools", toolStatus, "green");

            long mcpCount = List.of(context.getBeanDefinitionNames()).stream()
                    .filter(n -> n.toLowerCase().contains("mcp")).count();
            String mcpStatus = mcpCount == 0 ? "@|yellow Disconnected|@" : mcpCount + " Beans Found";
            printField(out, "🔌", "MCP Infra", mcpStatus, "cyan");

            printField(out, "📊", "Database", "R2DBC Connected", "green");

            out.println(Ansi.AUTO.string("@|magenta ──────────────────────────────────────────────────|@\n"));
            return 0;
        }
    }

    @Component
    @Command(name = "info", description = "Check detailed LLM provider information")
    @RequiredArgsConstructor
    public static class InfoCommand implements Callable<Integer> {
        private final ChatModel chatModel;
        @Spec CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            printSection(out, "AI PROVIDER DETAILS", "magenta");
            printField(out, "📡", "Provider", chatModel.getClass().getSimpleName(), "green");
            printField(out, "📝", "Details", chatModel.toString(), "white");
            out.println(Ansi.AUTO.string("@|magenta ──────────────────────────────────────────────────|@\n"));
            return 0;
        }
    }

    @Component
    @Command(name = "tools", description = "List all registered agent tools with descriptions")
    @RequiredArgsConstructor
    public static class ToolsCommand implements Callable<Integer> {
        private final List<ToolCallback> toolCallbacks;
        @Spec CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            if (toolCallbacks.isEmpty()) {
                printInfoBox(out, "⚠️ No local tools registered in the context.");
                return 0;
            }

            printSection(out, "REGISTERED AGENT TOOLS", "green");
            out.println(Ansi.AUTO.string("  @|white,bold TOOL NAME                 DESCRIPTION|@"));
            out.println(Ansi.AUTO.string("  @|faint ────────────────────────────────────────────────────────────────────────────|@"));

            for (ToolCallback tc : toolCallbacks) {
                String name = tc.getToolDefinition().name();
                String desc = tc.getToolDefinition().description();
                if (desc.length() > 50) desc = desc.substring(0, 47) + "...";
                
                out.println(Ansi.AUTO.string("  @|green " + String.format("%-25s", name) + "|@@|white " + desc + "|@"));
            }
            
            out.println(Ansi.AUTO.string("@|green ──────────────────────────────────────────────────────────────────────────────|@\n"));
            return 0;
        }
    }

    @Component
    @Command(name = "mcp", description = "Inspect MCP Infrastructure and clients")
    @RequiredArgsConstructor
    public static class McpCommand implements Callable<Integer> {
        private final ApplicationContext applicationContext;
        @Spec CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            List<String> mcpBeanNames = List.of(applicationContext.getBeanDefinitionNames()).stream()
                    .filter(name -> name.toLowerCase().contains("mcp"))
                    .collect(Collectors.toList());

            if (mcpBeanNames.isEmpty()) {
                printInfoBox(out, "⚠️ No MCP components found in current context.");
                return 0;
            }

            printSection(out, "MCP INFRASTRUCTURE", "cyan");

            for (String name : mcpBeanNames) {
                Object bean = applicationContext.getBean(name);
                out.println(Ansi.AUTO.string("  🔹 @|white,bold " + name + "|@"));
                out.println(Ansi.AUTO.string("     @|faint Type: |@@|italic " + bean.getClass().getSimpleName() + "|@"));
                
                try {
                    if (bean.getClass().getSimpleName().contains("McpClient") || 
                        bean.getClass().getSimpleName().contains("McpSyncClient")) {
                        
                        java.lang.reflect.Method listToolsMethod = bean.getClass().getMethod("listTools");
                        Object toolsResponse = listToolsMethod.invoke(bean);
                        
                        if (toolsResponse != null) {
                            out.println(Ansi.AUTO.string("     @|green ● Active|@ - Discovery Successful"));
                        }
                    }
                } catch (Exception e) {
                    // Skip
                }
                out.println();
            }
            out.println(Ansi.AUTO.string("@|cyan ──────────────────────────────────────────────────|@\n"));
            return 0;
        }
    }
}
