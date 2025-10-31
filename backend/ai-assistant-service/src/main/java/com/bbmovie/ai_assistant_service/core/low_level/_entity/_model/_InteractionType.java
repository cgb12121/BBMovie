package com.bbmovie.ai_assistant_service.core.low_level._entity._model;

public enum _InteractionType {
    USER_MESSAGE,
    AI_PARTIAL_RESPONSE,
    AI_COMPLETE_RESPONSE,
    TOOL_EXECUTION_REQUEST,
    TOOL_EXECUTION_RESULT,
    STREAMING_ERROR
}
