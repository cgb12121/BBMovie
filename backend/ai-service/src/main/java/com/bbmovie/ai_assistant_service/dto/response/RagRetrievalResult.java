package com.bbmovie.ai_assistant_service.dto.response;

import java.util.List;

public record RagRetrievalResult(
        String summaryText,                  // Text form for the LLM prompt
        List<RagMovieDto> documents         // Structured data for frontend or metadata extraction
) {}
