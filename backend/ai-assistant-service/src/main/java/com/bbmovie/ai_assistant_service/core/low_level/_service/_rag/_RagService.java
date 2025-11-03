package com.bbmovie.ai_assistant_service.core.low_level._service._rag;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface _RagService {
    Mono<List<String>> retrieveMovieContext(UUID sessionId, String query, int topK);
    Mono<Void> indexConversationFragment(UUID sessionId, String text);
}