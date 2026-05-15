package bbmovie.ai_platform.agentic_ai.dto;

import java.util.UUID;

/**
 * Value object representing a unique conversation identifier, 
 * composed of a User ID and a Session ID.
 * 
 * This object centralizes parsing and validation logic to avoid fragile string splitting.
 */
public record ConversationId(UUID userId, UUID sessionId) {

    private static final String DELIMITER = ":";

    /**
     * Parses a string in the format "userId:sessionId".
     * 
     * @param rawId The raw string from Spring AI ChatMemory.
     * @return A validated ConversationId object.
     * @throws IllegalArgumentException if the format is invalid.
     */
    public static ConversationId of(String rawId) {
        if (rawId == null || !rawId.contains(DELIMITER)) {
            throw new IllegalArgumentException("Invalid ConversationId format. Expected 'userId:sessionId' but got: " + rawId);
        }

        int delimIndex = rawId.indexOf(DELIMITER);
        String userIdStr = rawId.substring(0, delimIndex);
        String sessionIdStr = rawId.substring(delimIndex + 1);

        try {
            return new ConversationId(UUID.fromString(userIdStr), UUID.fromString(sessionIdStr));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID in ConversationId: " + rawId, e);
        }
    }

    @Override
    public String toString() {
        return userId.toString() + DELIMITER + sessionId.toString();
    }
}
