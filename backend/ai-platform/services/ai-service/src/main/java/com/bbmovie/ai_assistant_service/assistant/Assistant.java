package com.bbmovie.ai_assistant_service.assistant;

import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import reactor.core.publisher.Flux;

public interface Assistant {
    AssistantType getType();
    AssistantMetadata getInfo();
    Flux<ChatStreamChunk> processMessage(ChatContext chatContext);
}
