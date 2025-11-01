package com.bbmovie.ai_assistant_service.core.low_level._handler;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

public interface _ChatResponseHandlerFactory {
    StreamingChatResponseHandler create(
            UUID sessionId,
            ChatMemory memory,
            FluxSink<String> sink,
            MonoSink<Void> monoSink
    );
}
