package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.bbmovie.ai_assistant_service.dto.response.RagRetrievalResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface RagService {
    Mono<RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK);
    Mono<Void> indexConversationFragment(UUID sessionId, String text, List<RagMovieDto> pastResults);
}