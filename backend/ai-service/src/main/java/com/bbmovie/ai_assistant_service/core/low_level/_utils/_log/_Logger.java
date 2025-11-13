package com.bbmovie.ai_assistant_service.core.low_level._utils._log;

import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;

import static com.bbmovie.ai_assistant_service.core.low_level._utils._AnsiRainbowUtil.*;

/**
 * Utility class for logging with rainbow colors.
 * Uses SLF4J's {@link MessageFormatter} to format the message.
 * <p>
 * Kinda useless and shrink performance, but it's cool lol.
 */
public class _Logger implements Logger {

    @Delegate
    private final Logger slf4jLogger;

    public _Logger(Logger slf4jLogger) {
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