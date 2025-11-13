package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatSessionResponse;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface _SessionService {
    /**
     * Fetches a paginated list of all *active* sessions for a user,
     * using cursor-based pagination.
     *
     * @param userId   The ID of the user.
     * @param cursor   The Base64 encoded 'updated_at' timestamp to fetch items *before*.
     * @param size     The number of items to fetch.
     * @return A Mono emitting a cursor page of active session DTOs.
     */
    Mono<CursorPageResponse<_ChatSessionResponse>> activeSessionsWithCursor(UUID userId, String cursor, int size);

    /**
     * Creates a new chat session for a user.
     *
     * @param userId      The ID of the user creating the session.
     * @param sessionName The name for the new session.
     * @return A Mono emitting the created session DTO.
     */
    Mono<_ChatSessionResponse> createSession(UUID userId, String sessionName);

    /**
     * Deletes a chat session and all its associated messages.
     *
     * @param sessionId The ID of the session to delete.
     * @param userId    The ID of the user (for ownership validation).
     * @return A Mono<Void> that completes when the operation is done.
     */
    Mono<Void> deleteSession(UUID sessionId, UUID userId);

    /**
     * Retrieves a session by its ID and validates that the user owns it.
     * Throws an exception if the session is not found or the user is not the owner.
     *
     * @param sessionId The ID of the session to retrieve.
     * @param userId    The ID of the user to validate against.
     * @return A Mono emitting the chat session entity if validation passes.
     */
    Mono<_ChatSession> getAndValidateSessionOwnership(UUID sessionId, UUID userId);

    /**
     * Updates the name of a chat session.
     *
     * @param sessionId The ID of the session to update.
     * @param userId    The ID of the user (for ownership validation).
     * @param newName   The new name for the session.
     * @return A Mono emitting the updated session DTO.
     */
    Mono<_ChatSessionResponse> updateSessionName(UUID sessionId, UUID userId, String newName);

    /**
     * Sets the archived status of a chat session.
     * This is used for both archiving and unarchiving.
     *
     * @param sessionId  The ID of the session to update.
     * @param userId     The ID of the user (for ownership validation).
     * @param isArchived True to archive, false to unarchive.
     * @return A Mono emitting the updated session DTO.
     */
    Mono<_ChatSessionResponse> setSessionArchived(UUID sessionId, UUID userId, boolean isArchived);

    /**
     * Fetches a paginated list of all *archived* sessions for a user,
     * using cursor-based pagination.
     *
     * @param userId   The ID of the user.
     * @param cursor   The Base64 encoded 'updated_at' timestamp to fetch items *before*.
     * @param size     The number of items to fetch.
     * @return A Mono emitting a cursor page of archived session DTOs.
     */
    Mono<CursorPageResponse<_ChatSessionResponse>> listArchivedSessions(UUID userId, String cursor, int size);

    /**
     * Deletes a specific list of chat sessions, but *only if* they
     * are already archived and owned by the user.
     * <p>
     * This will also delete all associated chat messages.
     *
     * @param sessionIds A list of session IDs to delete.
     * @param userId     The ID of the user (for ownership validation).
     * @return A Mono<Void> that completes when the operation is done.
     */
    Mono<Void> deleteArchivedSessions(List<UUID> sessionIds, UUID userId);
}
