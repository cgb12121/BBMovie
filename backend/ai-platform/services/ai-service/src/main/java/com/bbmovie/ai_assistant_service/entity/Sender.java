package com.bbmovie.ai_assistant_service.entity;

import dev.langchain4j.data.message.ChatMessage;

public enum Sender {
    SYSTEM,
    USER,
    AI,
    AI_THINKING,
    TOOL_RESULT,
    TOOL_REQUEST,
    UNKNOWN;

    public static Sender fromLangchain(ChatMessage value) {
        return null;
    }
}
