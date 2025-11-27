package com.bbmovie.ai_assistant_service.config.ai.logging;

import com.bbmovie.ai_assistant_service.config.ai.logging.level.FormalLogging;
import com.bbmovie.ai_assistant_service.config.ai.logging.level.VerboseLogging;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggingDelegate implements Logging {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(LoggingDelegate.class);

    @Value("${ai.logging.mode:INFO}")
    private String mode;

    private final ChatModelListener delegate;

    public LoggingDelegate() {
        if ("VERBOSE".equalsIgnoreCase(mode)) {
            log.info("[AI-Listener] Using VERBOSE chat listener (ai.logging.mode={})", mode);
            this.delegate = new VerboseLogging();
        } else {
            log.info("[AI-Listener] Using COMPACT chat listener (ai.logging.mode={})", mode);
            this.delegate = new FormalLogging();
        }
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        delegate.onRequest(requestContext);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        delegate.onResponse(responseContext);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        delegate.onError(errorContext);
    }
}