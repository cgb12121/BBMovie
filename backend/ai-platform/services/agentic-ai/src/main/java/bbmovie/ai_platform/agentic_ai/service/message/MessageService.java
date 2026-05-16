package bbmovie.ai_platform.agentic_ai.service.message;

import bbmovie.ai_platform.agentic_ai.dto.response.ChatMessageResponse;
import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.entity.enums.Sender;

import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Contract for chat message retrieval and persistence.
 *
 * <p>Ownership checks are enforced per-method via {@code @CheckOwnership} in the implementation.
 */
public interface MessageService {

    Mono<CursorPageResponse<ChatMessageResponse>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size);

    Mono<ChatMessage> saveMessage(UUID sessionId, UUID userId, String content, Sender sender, UUID parentId);

    Mono<ChatMessage> getMessage(UUID messageId);
}
