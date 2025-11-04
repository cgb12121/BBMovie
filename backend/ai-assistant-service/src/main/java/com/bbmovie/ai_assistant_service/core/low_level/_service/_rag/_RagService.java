package com.bbmovie.ai_assistant_service.core.low_level._service._rag;

import com.bbmovie.ai_assistant_service.core.low_level._dto._RagMovieDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagRetrievalResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface _RagService {
    Mono<_RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK);
    Mono<Void> indexConversationFragment(UUID sessionId, String text, List<_RagMovieDto> pastResults);
}