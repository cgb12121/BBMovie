package com.bbmovie.ai_assistant_service.core.low_level._utils._log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create instances of CustomLogger.
 * This ensures each logger is tied to the correct class.
 */
public class _LoggerFactory {
    public static _Logger getLogger(Class<?> clazz) {
        Logger slf4jLogger = LoggerFactory.getLogger(clazz);
        return new _Logger(slf4jLogger);
    }
}