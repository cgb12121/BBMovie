package com.bbmovie.ai_assistant_service.core.low_level._config._db;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.r2dbc.proxy.core.*;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.Attribute.UNDERLINE;
import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
@Component
public class _R2dbcQueryLogger {

    @Bean
    @Profile({"default", "dev", "test", "docker"})
    public ProxyExecutionListener proxyExecutionListener() {
        return new ProxyExecutionListener() {
            @Override
            public void afterQuery(@NonNull QueryExecutionInfo info) {
                StringBuilder sb = new StringBuilder("\n");
                for (QueryInfo q : info.getQueries()) {
                    String formattedSql = formatSql(q.getQuery());
                    int maxLength = getMaxLineLength(formattedSql);

                    String sqlHeader = createCenteredHeader("SQL", maxLength);
                    appendRainbowUnderlined(sb, sqlHeader);
                    sb.append("\n");

                    String threadInfo = "Thread: " + info.getThreadName() + " (" + info.getThreadId() + ")";
                    appendRainbowUnderlined(sb, threadInfo);
                    sb.append("\n");

                    appendRainbow(sb, formattedSql);
                    sb.append("\n");

                    appendBindings(sb, q.getBindingsList());
                }

                Duration duration = info.getExecuteDuration();
                String durationText = duration.toMillis() + " ms";
                Ansi.Color durationColor = getDurationColor(duration);

                switch (durationColor) {
                    case GREEN -> {
                        appendRainbowUnderlined(sb, "Duration:");
                        sb.append(" ");
                        appendRainbow(sb, durationText);
                    }
                    case YELLOW, RED -> {
                        sb.append(ansi().fg(durationColor).a(UNDERLINE).a("Duration:").reset()).append(" ");
                        sb.append(ansi().fg(durationColor).a(durationText).reset());
                    }
                }
                sb.append("\n");

                if (info.getThrowable() != null) {
                    String errorMessage = info.getThrowable().getMessage();
                    sb.append(ansi().fgRed().a(UNDERLINE).a("Error: ").reset());
                    sb.append(ansi().fgRed().a(errorMessage).reset());
                    sb.append("\n");
                }

                log.debug(sb.toString());
            }
        };
    }

    private void appendRainbow(StringBuilder sb, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int[] rgb = getRainbowColor(i, text.length());
            sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(c).reset());
        }
    }

    private void appendRainbowUnderlined(StringBuilder sb, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int[] rgb = getRainbowColor(i, text.length());
            sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(UNDERLINE).a(c).reset());
        }
    }

    @SuppressWarnings("SameParameterValue")
    private String createCenteredHeader(String text, int maxWidth) {
        if (text.length() >= maxWidth) {
            return text;
        }
        int padding = maxWidth - text.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }

    private int getMaxLineLength(String text) {
        return text.lines()
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private Ansi.Color getDurationColor(Duration duration) {
        long millis = duration.toMillis();
        if (millis > 1000) return Ansi.Color.RED;
        if (millis > 300) return Ansi.Color.YELLOW;
        return Ansi.Color.GREEN;
    }

    private void appendBindings(StringBuilder sb, List<Bindings> bindingsList) {
        for (Bindings bindings : bindingsList) {
            if (!bindings.getIndexBindings().isEmpty()) {
                String indexBindings = bindings.getIndexBindings().stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(ansi().fgBrightGreen().a(UNDERLINE).a("Index Bindings:").reset())
                        .append("\n");
                appendRainbow(sb, indexBindings);
                sb.append("\n");
            }

            if (!bindings.getNamedBindings().isEmpty()) {
                String namedBindings = bindings.getNamedBindings().stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(ansi().fgBrightGreen().a(UNDERLINE).a("Named Bindings:").reset())
                        .append("\n");
                appendRainbow(sb, namedBindings);
                sb.append("\n");
            }
        }
    }

    @NonNull
    private String addPrefixForBinding(Binding binding) {
        return "===>" + formatBinding(binding);
    }

    private String formatBinding(Binding b) {
        BoundValue bv = b.getBoundValue();
        Object key = b.getKey();

        if (bv.isNull())
            return "[" + key + "=NULL]";

        Object v = bv.getValue();
        String value = (v instanceof String || v instanceof Temporal)
                ? "'" + v + "'"
                : String.valueOf(v);

        return "[" + key + "=" + value + "]";
    }

    private String formatSql(String sql) {
        return SqlFormatter
                .of(Dialect.MySql)
                .format(sql);
    }

    private static int[] getRainbowColor(int index, int totalLength) {
        double position = (double) index / totalLength;
        int red = (int) (Math.sin(position * Math.PI * 2 + 0) * 127 + 128);
        int green = (int) (Math.sin(position * Math.PI * 2 + 2 * Math.PI / 3) * 127 + 128);
        int blue = (int) (Math.sin(position * Math.PI * 2 + 4 * Math.PI / 3) * 127 + 128);
        return new int[]{red, green, blue};
    }
}