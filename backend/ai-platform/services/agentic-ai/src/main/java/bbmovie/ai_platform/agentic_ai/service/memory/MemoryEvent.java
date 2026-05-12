package bbmovie.ai_platform.agentic_ai.service.memory;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Event hold information to sync Memory.
 */
public record MemoryEvent(
    EventType type,
    String sessionId,
    String userId,
    List<String> jsonMessages, // Hold a list of message in raw JSON
    Instant timestamp,
    String metadata // Optional data (parentId, model, etc.)
) implements Serializable {

    public enum EventType {
        ADD,    // Add new message
        CLEAR,  // Clear session
        DELETE  // Delete message
    }

    public static MemoryEvent add(String sessionId, String userId, List<String> jsonMessages) {
        return new MemoryEvent(EventType.ADD, sessionId, userId, jsonMessages, Instant.now(), null);
    }

    public static MemoryEvent clear(String sessionId, String userId) {
        return new MemoryEvent(EventType.CLEAR, sessionId, userId, null, Instant.now(), null);
    }
}
