package com.bbmovie.ai_assistant_service.core.low_level._config._ai._logging;

import dev.langchain4j.model.chat.listener.ChatModelListener;

/**
 * Marker interface to isolate our AI chat listeners from
 * any framework-provided ChatModelListener beans.
 * <p>
 * This allows precise Spring injection like:
 *     public StreamingChatModel _ThinkingModel(AiChatListener listener)
 */
public interface _Logging extends ChatModelListener {
    // Marker interface
    // This interface can be used to trigger business logic in our listener like saving request/response to the database
}
