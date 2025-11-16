package com.bbmovie.ai_assistant_service.utils.log;

/**
 * Factory to create instances of CustomLogger.
 * This ensures each logger is tied to the correct class.
 */
public class LoggerFactory {
    public static Logger getLogger(Class<?> clazz) {
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(clazz);
        return new Logger(slf4jLogger);
    }
}