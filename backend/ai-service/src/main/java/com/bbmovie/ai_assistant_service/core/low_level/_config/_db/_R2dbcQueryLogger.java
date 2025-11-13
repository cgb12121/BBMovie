package com.bbmovie.ai_assistant_service.core.low_level._config._db;

import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.r2dbc.proxy.core.*;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import org.fusesource.jansi.Ansi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.stream.Collectors;

import static com.bbmovie.ai_assistant_service.core.low_level._utils._AnsiRainbowUtil.*;
import static org.fusesource.jansi.Ansi.Attribute.UNDERLINE;
import static org.fusesource.jansi.Ansi.ansi;

@Component
public class _R2dbcQueryLogger {

    private static final _Logger log = _LoggerFactory.getLogger(_R2dbcQueryLogger.class);

    @Bean
    @Profile({"default"})
    public ProxyExecutionListener proxyExecutionListener() {
        return new ProxyExecutionListener() {
            @Override
            public void afterQuery(@NonNull QueryExecutionInfo info) {
                StringBuilder sb = new StringBuilder("\n");
                for (QueryInfo q : info.getQueries()) {
                    String formattedSql = formatSql(q.getQuery());
                    int maxLength = getMaxLineLength(formattedSql);

                    String sqlHeader = createCenteredHeader("SQL", maxLength);
                    sb.append(getLightRainbowUnderlined(sqlHeader));
                    sb.append("\n");

                    String threadInfo = "Thread: " + info.getThreadName() + " (" + info.getThreadId() + ")";
                    sb.append(getLightRainbowUnderlined(threadInfo));
                    sb.append("\n");

                    sb.append(getLightRainbow(formattedSql));
                    sb.append("\n");

                    appendBindings(sb, q.getBindingsList());
                }

                Duration duration = info.getExecuteDuration();
                String durationText = duration.toMillis() + " ms";
                Ansi.Color durationColor = getDurationColor(duration);

                switch (durationColor) {
                    case GREEN -> {
                        sb.append(getLightRainbowUnderlined("Duration:"));
                        sb.append(" ");
                        sb.append(getLightRainbow(durationText));
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
                String indexBindings = bindings.getIndexBindings()
                        .stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(getLightRainbowUnderlined("Index Bindings:"))
                        .append("\n");
                sb.append(getLightRainbow(indexBindings));
                sb.append("\n");
            }

            if (!bindings.getNamedBindings().isEmpty()) {
                String namedBindings = bindings.getNamedBindings().stream()
                        .map(this::addPrefixForBinding)
                        .collect(Collectors.joining("\n"));

                sb.append(getLightRainbowUnderlined("Named Bindings:"))
                        .append("\n");
                sb.append(getLightRainbow(namedBindings));
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
}