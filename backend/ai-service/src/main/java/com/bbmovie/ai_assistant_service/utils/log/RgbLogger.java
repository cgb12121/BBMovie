package com.bbmovie.ai_assistant_service.utils.log;

import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;

import static com.bbmovie.ai_assistant_service.utils.log.OptimizedAnsiRainbowUtil.*;

/**
 * Utility class for logging with rainbow colors.
 * Uses SLF4J's {@link MessageFormatter} to format the message.
 * <p>
 * Kinda useless and shrink performance, but it's cool lol.
 */
public record RgbLogger(@Delegate Logger slf4jLogger) implements Logger {

    @Override
    public void info(String format, Object... args) {
        if (!slf4jLogger.isInfoEnabled()) return;
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getLightRainbow(ft.getMessage());
        slf4jLogger.info(coloredMessage, ft.getThrowable());
    }

    @Override
    public void info(String msg) {
        info(msg, (Object[]) null);
    }

    @Override
    public void info(String format, Object arg) {
        info(format, new Object[]{arg});
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        info(format, new Object[]{arg1, arg2});
    }

    @Override
    public void debug(String format, Object... args) {
        if (!slf4jLogger.isDebugEnabled()) return;
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getLightRainbow(ft.getMessage());
        slf4jLogger.debug(coloredMessage, ft.getThrowable());
    }

    @Override
    public void debug(String msg) {
        debug(msg, (Object[]) null);
    }

    @Override
    public void debug(String format, Object arg) {
        debug(format, new Object[]{arg});
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        debug(format, new Object[]{arg1, arg2});
    }

    @Override
    public void error(String format, Object... args) {
        if (!slf4jLogger.isErrorEnabled()) return;
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getErrorLightRed(ft.getMessage());
        slf4jLogger.error(coloredMessage, ft.getThrowable());
    }

    @Override
    public void error(String msg) {
        error(msg, (Object[]) null);
    }

    @Override
    public void error(String format, Object arg) {
        error(format, new Object[]{arg});
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        error(format, new Object[]{arg1, arg2});
    }

    @Override
    public void trace(String format, Object... args) {
        if (!slf4jLogger.isTraceEnabled()) return;
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getFullRainbow(ft.getMessage());
        slf4jLogger.trace(coloredMessage, ft.getThrowable());
    }

    @Override
    public void trace(String msg) {
        trace(msg, (Object[]) null);
    }

    @Override
    public void trace(String format, Object arg) {
        trace(format, new Object[]{arg});
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        trace(format, new Object[]{arg1, arg2});
    }

    @Override
    public void warn(String format, Object... args) {
        if (!slf4jLogger.isWarnEnabled()) return;
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getWarningOrangeToYellow(ft.getMessage());
        slf4jLogger.warn(coloredMessage, ft.getThrowable());
    }

    @Override
    public void warn(String msg) {
        warn(msg, (Object[]) null);
    }

    @Override
    public void warn(String format, Object arg) {
        warn(format, new Object[]{arg});
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warn(format, new Object[]{arg1, arg2});
    }
}