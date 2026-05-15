package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

/**
 * ChatServiceImpl provides the high-level implementation for AI interaction.
 * 
 * It coordinates between {@link ChatRequestFactory} for building complex requests
 * and {@link MessageService} for message retrieval and regeneration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final MessageService messageService;
    private final ChatRequestFactory requestFactory;

    /**
     * Initiates a chat request to the AI.
     * 
     * Request building logic is delegated to {@link ChatRequestFactory} to handle
     * personalization, tool injection, and model-specific configurations.
     */
    @Override
    public Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId, UUID assetId, AiMode mode, AiModel model) {
        log.info("[Chat] Session: {}, Mode: {}, Model: {}, Asset: {}", sessionId, mode, model, assetId);
        
        return requestFactory.createRequest(sessionId, userId, message, assetId, mode, model)
                .flatMapMany(spec -> spec.stream().content());
    }

    /**
     * Edits an existing message and triggers a new AI response.
     */
    @Override
    public Flux<String> editMessage(UUID oldMessageId, UUID userId, String newContent) {
        return messageService.getMessage(oldMessageId)
                .flatMapMany(oldMsg -> chat(oldMsg.getSessionId(), userId, newContent, oldMsg.getParentId(), null, AiMode.NORMAL, null));
    }

    /**
     * Regenerates an AI response based on the previous user message.
     */
    @Override
    public Flux<String> regenerateMessage(UUID aiMessageId, UUID userId) {
        return messageService.getMessage(aiMessageId)
                .flatMap(aiMsg -> messageService.getMessage(aiMsg.getParentId()))
                .flatMapMany(userMsg -> {
                    // Manual logs are not deleted here as the ThinkingAdvisor/Memory system 
                    // handles the persistence of the new response.
                    return chat(userMsg.getSessionId(), userId, userMsg.getContent(), userMsg.getParentId(), null, AiMode.NORMAL, null);
                });
    }
}
