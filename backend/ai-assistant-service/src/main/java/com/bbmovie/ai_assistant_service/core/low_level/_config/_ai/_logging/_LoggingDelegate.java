package com.bbmovie.ai_assistant_service.core.low_level._config._ai._logging;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._logging.level._FormalLogging;
import com.bbmovie.ai_assistant_service.core.low_level._config._ai._logging.level._VerboseLogging;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class _LoggingDelegate implements _Logging {

    @Value("${ai.logging.mode:INFO}")
    private String mode;

    private final ChatModelListener delegate;

    public _LoggingDelegate() {
        if ("VERBOSE".equalsIgnoreCase(mode)) {
            log.info("[AI-Listener] Using VERBOSE chat listener (ai.logging.mode={})", mode);
            this.delegate = new _VerboseLogging();
        } else {
            log.info("[AI-Listener] Using COMPACT chat listener (ai.logging.mode={})", mode);
            this.delegate = new _FormalLogging();
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