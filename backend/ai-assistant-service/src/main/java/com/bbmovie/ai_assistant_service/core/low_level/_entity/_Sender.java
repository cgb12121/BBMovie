package com.bbmovie.ai_assistant_service.core.low_level._entity;

import dev.langchain4j.data.message.ChatMessage;

public enum _Sender {
    SYSTEM,
    USER,
    AI,
    AI_THINKING,
    TOOL_RESULT,
    TOOL_REQUEST,
    UNKNOWN;

    public static _Sender fromLangchain(ChatMessage value) {
        return null;
    }
}
