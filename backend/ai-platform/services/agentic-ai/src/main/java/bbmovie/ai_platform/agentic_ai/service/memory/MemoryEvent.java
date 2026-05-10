package bbmovie.ai_platform.agentic_ai.service.memory;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Event chứa thông tin đồng bộ Memory.
 */
public record MemoryEvent(
    EventType type,
    String sessionId,
    String userId,
    List<String> jsonMessages, // Chứa list tin nhắn dạng JSON
    Instant timestamp,
    String metadata // Chứa thêm thông tin phụ nếu cần (parentId, model, etc.)
) implements Serializable {

    public enum EventType {
        ADD,    // Thêm tin nhắn mới
        CLEAR,  // Xóa toàn bộ session
        DELETE  // Xóa một tin nhắn cụ thể (nếu mở rộng)
    }

    public static MemoryEvent add(String sessionId, String userId, List<String> jsonMessages) {
        return new MemoryEvent(EventType.ADD, sessionId, userId, jsonMessages, Instant.now(), null);
    }

    public static MemoryEvent clear(String sessionId, String userId) {
        return new MemoryEvent(EventType.CLEAR, sessionId, userId, null, Instant.now(), null);
    }
}
