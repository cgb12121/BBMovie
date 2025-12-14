package com.bbmovie.ai_assistant_service.repository.custom;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

/**
 * This interface defines the contract for our custom, complex queries
 * that Spring Data's auto-generation can't handle.
 */
public interface ChatSessionRepositoryCustom {

    /**
     * Fetches active (non-archived) sessions for a user, starting after a given
     * time, with a specified limit.
     *
     * @param userId     The user's ID.
     * @param cursorTime The 'updated_at' timestamp to fetch items *before*.
     * @param limit      The number of items to fetch.
     * @return A Flux of chat sessions.
     */
    Flux<ChatSession> findActiveSessionsWithCursor(UUID userId, Instant cursorTime, int limit);

    /**
     * Fetches deactivated (archived) sessions for a user, starting after a given
     * time, with a specified limit.
     *
     * @param userId     The user's ID.
     * @param cursorTime The 'updated_at' timestamp to fetch items *before*.
     * @param limit      The number of items to fetch.
     * @return A Flux of chat sessions.
     */
    Flux<ChatSession> findArchivedSessionsWithCursor(UUID userId, Instant cursorTime, int limit);
}