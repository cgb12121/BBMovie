package com.bbmovie.ai_assistant_service.utils.log;

import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.function.BiConsumer;

public record OptimizedRgbLogger(@Delegate Logger slf4jLogger) implements Logger {

    private void log(boolean enabled, BiConsumer<String, Throwable> sink, String format, Object... args) {
        if (!enabled) return;

        if (args == null || args.length == 0) {
            sink.accept(color(format), null);
            return;
        }

        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        sink.accept(color(ft.getMessage()), ft.getThrowable());
    }

    @Override
    public void info(String format, Object... args) {
        log(slf4jLogger.isInfoEnabled(), slf4jLogger::info, format, args);
    }

    @Override public void info(String msg) { info(msg, (Object[]) null); }

    @Override
    public void debug(String format, Object... args) {
        log(slf4jLogger.isDebugEnabled(), slf4jLogger::debug, format, args);
    }

    @Override public void debug(String msg) { debug(msg, (Object[]) null); }

    @Override
    public void warn(String format, Object... args) {
        log(slf4jLogger.isWarnEnabled(), slf4jLogger::warn, format, args);
    }

    @Override public void warn(String msg) { warn(msg, (Object[]) null); }

    @Override
    public void error(String format, Object... args) {
        log(slf4jLogger.isErrorEnabled(), slf4jLogger::error, format, args);
    }

    @Override public void error(String msg) { error(msg, (Object[]) null); }

    @Override
    public void trace(String format, Object... args) {
        log(slf4jLogger.isTraceEnabled(), slf4jLogger::trace, format, args);
    }

    @Override public void trace(String msg) { trace(msg, (Object[]) null); }

    private static String color(String msg) {
        return OptimizedAnsiRainbowUtil.getLightRainbow(msg);
    }
}
