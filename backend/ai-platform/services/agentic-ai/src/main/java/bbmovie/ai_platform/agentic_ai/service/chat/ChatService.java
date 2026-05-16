package bbmovie.ai_platform.agentic_ai.service.chat;

import java.util.UUID;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import reactor.core.publisher.Flux;

public interface ChatService {
     Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId, UUID assetId, AiMode mode, AiModel model, String userRole);

     Flux<String> regenerateMessage(UUID messageId, UUID userId);

     Flux<String> editMessage(UUID messageId, UUID userId, String newContent);
}
