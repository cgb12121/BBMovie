package com.bbmovie.ai_assistant_service.entity.model;

public enum InteractionType {
    USER_MESSAGE,
    EMBEDDING,
    RETRIEVAL,
    EMBEDDING_INDEX,
    AI_PARTIAL_RESPONSE,
    AI_COMPLETE_RESULT,
    TOOL_EXECUTION_REQUEST,
    TOOL_EXECUTION_RESULT,
    ERROR
}
