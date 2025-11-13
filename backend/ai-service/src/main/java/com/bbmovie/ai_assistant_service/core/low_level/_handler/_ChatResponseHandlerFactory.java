package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

@FunctionalInterface
public interface _ChatResponseHandlerFactory {
    StreamingChatResponseHandler create(
            UUID sessionId,
            ChatMemory memory,
            FluxSink<_ChatStreamChunk> sink,
            MonoSink<Void> monoSink,
            _AiMode aiMode
    );
}
