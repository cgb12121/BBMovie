package com.bbmovie.ai_assistant_service.core.low_level._handler._processor;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import reactor.core.publisher.Mono;

public interface _ResponseProcessor {
    Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata);
}