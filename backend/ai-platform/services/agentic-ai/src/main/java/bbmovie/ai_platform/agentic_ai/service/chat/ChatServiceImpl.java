package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.config.AiTimeoutProperties;
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
 * <p>Coordinates between {@link ChatRequestFactory} for building complex requests
 * and {@link MessageService} for message retrieval and regeneration.
 *
 * <p><b>Timeout handling:</b> Each stream is wrapped with a per-mode timeout
 * (configured via {@link AiTimeoutProperties}). On timeout, a sentinel SSE string
 * {@code "[STREAM_TIMEOUT]"} is emitted so the client can display an appropriate message.
 * We emit rather than throw because exceptions in SSE streams are swallowed by the
 * framework and the client would see an abrupt connection close with no context.
 *
 * <p><b>Resilience:</b> Circuit breaking and retry are applied by {@link ResilientChatService}
 * via the Decorator pattern — this class contains only business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageService messageService;
    private final ChatRequestFactory requestFactory;
    private final AiTimeoutProperties timeoutProperties;

    /**
     * Sentinel token emitted to the SSE stream when the AI provider exceeds its timeout.
     * The client should detect this string and display a user-friendly timeout message.
     */
    public static final String STREAM_TIMEOUT_SENTINEL = "[STREAM_TIMEOUT]";

    /**
     * Initiates a chat request to the AI with a per-mode streaming timeout.
     */
    @Override
    public Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId, UUID assetId, AiMode mode, AiModel model, String userRole) {
        log.info("[Chat] Session: {}, Mode: {}, Model: {}, Asset: {}", sessionId, mode, model, assetId);

        return requestFactory.createRequest(sessionId, userId, message, assetId, mode, model, userRole)
                .flatMapMany(spec -> spec.stream().content())
                .timeout(
                    timeoutProperties.getTimeoutForMode(mode),
                    Flux.just(STREAM_TIMEOUT_SENTINEL)
                );
    }

    /**
     * Edits an existing message and triggers a new AI response.
     */
    @Override
    public Flux<String> editMessage(UUID oldMessageId, UUID userId, String newContent) {
        return messageService.getMessage(oldMessageId)
                .flatMapMany(oldMsg -> chat(
                    oldMsg.getSessionId(), userId, newContent,
                    oldMsg.getParentId(), null, AiMode.NORMAL, null, null  // role not available on edit
                ));
    }

    /**
     * Regenerates an AI response based on the previous user message.
     */
    @Override
    public Flux<String> regenerateMessage(UUID aiMessageId, UUID userId) {
        return messageService.getMessage(aiMessageId)
                .flatMap(aiMsg -> messageService.getMessage(aiMsg.getParentId()))
                .flatMapMany(userMsg ->
                    // Manual logs are not deleted here — ThinkingAdvisor/Memory handles persistence.
                    chat(userMsg.getSessionId(), userId, userMsg.getContent(),
                         userMsg.getParentId(), null, AiMode.NORMAL, null, null)  // role not available on regenerate
                );
    }
}
