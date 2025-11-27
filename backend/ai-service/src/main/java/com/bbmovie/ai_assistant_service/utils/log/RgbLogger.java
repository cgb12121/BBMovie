package com.bbmovie.ai_assistant_service.utils.log;

import lombok.experimental.Delegate;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;

import static com.bbmovie.ai_assistant_service.utils.log.AnsiRainbowUtil.*;

/**
 * Utility class for logging with rainbow colors.
 * Uses SLF4J's {@link MessageFormatter} to format the message.
 * <p>
 * Kinda useless and shrink performance, but it's cool lol.
 */
public class RgbLogger implements org.slf4j.Logger {

    @Delegate
    private final org.slf4j.Logger slf4jLogger;

    public RgbLogger(org.slf4j.Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
    }

    @Override
    public void info(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getLightRainbow(ft.getMessage());
        slf4jLogger.info(coloredMessage, ft.getThrowable());
    }

    @Override
    public void debug(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getLightRainbow(ft.getMessage());
        slf4jLogger.debug(coloredMessage, ft.getThrowable());
    }

    @Override
    public void error(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getErrorLightRed(ft.getMessage());
        slf4jLogger.error(coloredMessage, ft.getThrowable());
    }

    @Override
    public void trace(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getFullRainbow(ft.getMessage());
        slf4jLogger.trace(coloredMessage, ft.getThrowable());
    }

    @Override
    public void warn(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        String coloredMessage = getWarningOrangeToYellow(ft.getMessage());
        slf4jLogger.warn(coloredMessage, ft.getThrowable());
    }
}