package com.bbmovie.ai_assistant_service.core.low_level._config._db;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.r2dbc.proxy.core.*;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.springframework.context.annotation.Bean;
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

    @Bean("_ProxyExecutionListener")
    public ProxyExecutionListener proxyExecutionListener() {
        return new ProxyExecutionListener() {
            @Override
            public void afterQuery(@NonNull QueryExecutionInfo info) {
                StringBuilder sb = new StringBuilder("\n");
                for (QueryInfo q : info.getQueries()) {
                    String formattedSql = formatSql(q.getQuery());

                    int maxLength = formattedSql.lines()
                            .mapToInt(String::length)
                            .max()
                            .orElse(0);
                    StringBuilder sqlHeader = new StringBuilder();
                    sqlHeader.append(" ".repeat(Math.max(0, maxLength / 2)));
                    sqlHeader.append("SQL");
                    sqlHeader.append(" ".repeat(Math.max(0, maxLength - maxLength / 2)));
                    for (int i = 0; i < sqlHeader.length(); i++) {
                        char c = sqlHeader.charAt(i);
                        int[] rgb = getRainbowColor(i, sqlHeader.length());
                        sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(UNDERLINE).a(c).reset());
                    }
                    sb.append("\n");

                    for (int i = 0; i < formattedSql.length(); i++) {
                        char c = formattedSql.charAt(i);
                        int[] rgb = getRainbowColor(i, formattedSql.length());

                        sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(c).reset());
                    }
                    sb.append("\n");
                    appendBindings(sb, q.getBindingsList());
                }

                Duration duration = info.getExecuteDuration();
                String durationText = duration.toMillis() + " ms";

                Ansi.Color durationColor;
                 if (duration.toMillis() > 1000) {
                    durationColor = Ansi.Color.RED;
                } else if (duration.toMillis() > 300) {
                    durationColor = Ansi.Color.YELLOW;
                } else {
                     durationColor = Ansi.Color.GREEN;
                 }

                switch (durationColor) {
                    case GREEN -> {
                        String header = "Duration:";
                        for (int i = 0; i < header.length(); i++) {
                            char c = header.charAt(i);
                            int[] rgb = getRainbowColor(i, header.length());
                            sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(UNDERLINE).a(c).reset());
                        }
                        sb.append(" ");
                        for (int i = 0; i < durationText.length(); i++) {
                            char c = durationText.charAt(i);
                            int[] rgb = getRainbowColor(i, durationText.length());
                            sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(c).reset());
                        }
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

    private void appendBindings(StringBuilder sb, List<Bindings> bindingsList) {
        for (Bindings bindings : bindingsList) {
            if (!bindings.getIndexBindings().isEmpty()) {
                String indexBindings = bindings.getIndexBindings().stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(ansi().fgBrightGreen().a(UNDERLINE).a("Index Bindings:").reset())
                        .append("\n");

                for (int i = 0; i < indexBindings.length(); i++) {
                    char c = indexBindings.charAt(i);
                    int[] rgb = getRainbowColor(i, indexBindings.length());
                    sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(c).reset());
                }
                sb.append("\n");
            }

            if (!bindings.getNamedBindings().isEmpty()) {
                String namedBindings = bindings.getNamedBindings().stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(ansi().fgBrightGreen().a(UNDERLINE).a("Named Bindings:").reset());
                for (int i = 0; i < namedBindings.length(); i++) {
                    char c = namedBindings.charAt(i);
                    int[] rgb = getRainbowColor(i, namedBindings.length());
                    sb.append(ansi().fgRgb(rgb[0], rgb[1], rgb[2]).a(c).reset());
                }
                sb.append("\n");
            }
        }
    }

    private @org.jspecify.annotations.NonNull String addPrefixForBinding(Binding binding) {
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
