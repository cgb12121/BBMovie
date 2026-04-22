package com.bbmovie.ai_assistant_service.dto;

import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

@Value
@Builder
public class ToolExecutionContext {
    @NonNull UUID sessionId;
    String userId; // Added for HITL
    @NonNull AiMode aiMode;
    @NonNull AiMessage aiMessage;
    @NonNull ChatMemory chatMemory;
    ToolsRegistry toolRegistry;
    @NonNull SystemMessage systemPrompt;
    @NonNull FluxSink<ChatStreamChunk> sink;
    long requestStartTime;
    ChatResponseMetadata responseMetadata;
    String internalApprovalToken; // Added for HITL
    String messageId; // Added for HITL binding
}
