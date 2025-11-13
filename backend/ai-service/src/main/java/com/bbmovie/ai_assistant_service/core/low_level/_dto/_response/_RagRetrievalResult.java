package com.bbmovie.ai_assistant_service.core.low_level._dto._response;

import java.util.List;

public record _RagRetrievalResult(
        String summaryText,                  // Text form for the LLM prompt
        List<_RagMovieDto> documents         // Structured data for frontend or metadata extraction
) {}
