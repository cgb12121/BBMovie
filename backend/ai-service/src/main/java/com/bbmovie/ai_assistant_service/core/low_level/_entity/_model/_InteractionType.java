package com.bbmovie.ai_assistant_service.core.low_level._entity._model;

public enum _InteractionType {
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
