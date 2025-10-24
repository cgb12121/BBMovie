package com.bbmovie.ai_assistant_service.utils;

import com.bbmovie.ai_assistant_service.domain.entity.ChatMessageEntity;
import com.bbmovie.ai_assistant_service.domain.entity.MessageRole;
import dev.langchain4j.data.message.*;

import java.time.LocalDateTime;

public class LangchainMapper {

    public static ChatMessage toLangChain4jChatMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case USER -> new UserMessage(entity.getContent());
            case AI -> new AiMessage(entity.getContent());
        };
    }

    public static ChatMessageEntity toChatMessageEntity(ChatMessage chatMessage, Long sessionId) {
        String content = null;
        String thinking = null;
        String toolUsage = null;
        MessageRole role = switch (chatMessage) {
            case UserMessage userMessage -> {
                content = userMessage.singleText();
                yield MessageRole.USER;
            }
            case AiMessage aiMessage -> {
                content = aiMessage.text();
                thinking = aiMessage.thinking();
                yield MessageRole.AI;
            }
            case ToolExecutionResultMessage toolExecutionResultMessage -> {
                toolUsage = toolExecutionResultMessage.toolName();
                yield MessageRole.AI;
            }
            case SystemMessage ignored -> MessageRole.AI;
            case CustomMessage ignored -> MessageRole.AI;
            case null, default -> throw new IllegalStateException("Unexpected value: " + chatMessage);
        };

        return ChatMessageEntity.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .thinking(thinking)
                .toolUsage(toolUsage)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
