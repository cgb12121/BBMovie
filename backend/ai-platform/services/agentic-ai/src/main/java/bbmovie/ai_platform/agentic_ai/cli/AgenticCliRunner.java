package bbmovie.ai_platform.agentic_ai.cli;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Help.Ansi;

import java.io.PrintWriter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgenticCliRunner implements CommandLineRunner, ExitCodeGenerator {

    private final AgenticShell agenticShell;
    private final IFactory factory;

    private int exitCode;

    @Override
    public void run(String... args) {
        // Run as command if args provided, else just start shell
        if (args.length > 0) {
            exitCode = new CommandLine(agenticShell, factory).execute(args);
        }
        startInteractiveShell();
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void startInteractiveShell() {
        Thread.ofVirtual().start(() -> {
            try {
                // Start Terminal with JLine
                Terminal terminal = TerminalBuilder.builder()
                        .system(true)
                        .build();

                CommandLine cmd = new CommandLine(agenticShell, factory);
                // Redirect picocli output to jline terminal
                cmd.setOut(new PrintWriter(terminal.writer(), true));
                cmd.setErr(new PrintWriter(terminal.writer(), true));
                
                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .appName("AgenticAI")
                        .build();
                
                String prompt = Ansi.AUTO.string("@|cyan,bold agentic-ai|@@|white > |@");
                
                terminal.writer().println(Ansi.AUTO.string("\n@|green,bold 🤖 Agentic AI Premium Shell (JLine 3) started.|@"));
                terminal.writer().println(Ansi.AUTO.string("@|faint Type 'exit' to quit, 'help' for commands.|@\n"));

                while (true) {
                    String line;
                    try {
                        line = reader.readLine(prompt);
                    } catch (UserInterruptException | EndOfFileException e) {
                        String err = new String("@|red,bold %d .|@").formatted(e.getMessage());
                        terminal.writer().print(Ansi.AUTO.string(err));
                        break;
                    }

                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    if ("exit".equalsIgnoreCase(trimmed) || "quit".equalsIgnoreCase(trimmed)) {
                        break;
                    }

                    // Execute command
                    String[] cmdArgs = trimmed.split("\\s+");
                    cmd.execute(cmdArgs);
                }
                
                terminal.writer().println(Ansi.AUTO.string("@|yellow Goodbye!|@"));
                terminal.close();

            } catch (Exception e) {
                log.error("Error in interactive shell: {}", e.getMessage());
            }
        });
    }
}
