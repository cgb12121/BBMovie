package bbmovie.ai_platform.agentic_ai.cli;

import lombok.experimental.UtilityClass;
import picocli.CommandLine.Help.Ansi;

import java.io.PrintWriter;

/**
 * Utility class for premium CLI styling using Picocli Ansi markup.
 * This ensures that ANSI colors are only used when the terminal supports them
 * and avoids raw escape characters in logs or redirected output.
 */
@UtilityClass
public class CliAnsiUtils {

    /**
     * Prints a premium ASCII Art header using Picocli markup.
     */
    public static void printHeader(PrintWriter out) {
        out.println(Ansi.AUTO.string("@|cyan,bold  █████╗  ██████╗ ███████╗███╗   ██╗████████╗██╗ ██████╗     █████╗ ██╗|@"));
        out.println(Ansi.AUTO.string("@|cyan,bold ██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝██║██╔════╝    ██╔══██╗██║|@"));
        out.println(Ansi.AUTO.string("@|cyan,bold ███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║   ██║██║         ███████║██║|@"));
        out.println(Ansi.AUTO.string("@|cyan,bold ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║   ██║██║         ██╔══██║██║|@"));
        out.println(Ansi.AUTO.string("@|cyan,bold ██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║   ██║╚██████╗    ██║  ██║██║|@"));
        out.println(Ansi.AUTO.string("@|cyan,bold ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝   ╚═╝ ╚═════╝    ╚═╝  ╚═╝╚═╝|@"));
        out.println(Ansi.AUTO.string("@|faint    --- The Intelligent Agentic AI Management System ---|@\n"));
    }

    /**
     * Prints a section title with a separator.
     */
    public static void printSection(PrintWriter out, String title, String colorMarkup) {
        String line = "─".repeat(Math.max(0, 50 - title.length() - 4));
        out.println(Ansi.AUTO.string("@|" + colorMarkup + ",bold ── " + title + " " + line + "|@"));
    }

    /**
     * Prints a labeled value with styling.
     */
    public static void printField(PrintWriter out, String icon, String label, String value, String valueMarkup) {
        String formattedLabel = String.format("%-12s", label + ":");
        out.print("  " + icon + " " + Ansi.AUTO.string("@|white,bold " + formattedLabel + "|@") + " ");
        
        // Tránh lồng markup nếu value đã chứa markup rồi
        if (value.startsWith("@|")) {
            out.println(Ansi.AUTO.string(value));
        } else {
            out.println(Ansi.AUTO.string("@|" + valueMarkup + " " + value + "|@"));
        }
    }

    /**
     * Formats a box for information display.
     */
    public static void printInfoBox(PrintWriter out, String message) {
        String horizontal = "─".repeat(message.length() + 2);
        out.println(Ansi.AUTO.string("@|yellow ┌" + horizontal + "┐|@"));
        out.println(Ansi.AUTO.string("@|yellow │ |@@|white " + message + "|@@|yellow  │|@"));
        out.println(Ansi.AUTO.string("@|yellow └" + horizontal + "┘|@"));
    }
}
