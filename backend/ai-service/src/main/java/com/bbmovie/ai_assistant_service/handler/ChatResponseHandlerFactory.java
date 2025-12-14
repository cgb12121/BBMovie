package com.bbmovie.ai_assistant_service.handler;

import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

@FunctionalInterface
public interface ChatResponseHandlerFactory {
    StreamingChatResponseHandler create(
            UUID sessionId,
            ChatMemory memory,
            FluxSink<ChatStreamChunk> sink,
            MonoSink<Void> monoSink,
            AiMode aiMode,
            ChatContext context // Added Context
    );
}