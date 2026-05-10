package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.agentic_ai.entity.Sender;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatClient chatClient;
    private final MessageService messageService;
    private final ChatRequestFactory requestFactory;

    /**
     * Thực hiện chat với AI.
     * Logic xây dựng Request đã được tách ra ChatRequestFactory để đảm bảo Fail-Fast và Caching.
     */
    public Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId, AiMode mode, AiModel model) {
        log.info("[Chat] Session: {}, Mode: {}, Model: {}", sessionId, mode, model);
        
        return requestFactory.createRequest(chatClient, sessionId, userId, message, mode, model)
                .stream()
                .content();
    }

    public Flux<String> editMessage(UUID oldMessageId, UUID userId, String newContent) {
        return messageService.getMessage(oldMessageId)
                .flatMapMany(oldMsg -> chat(oldMsg.getSessionId(), userId, newContent, oldMsg.getParentId(), AiMode.NORMAL, null));
    }

    public Flux<String> regenerateMessage(UUID aiMessageId, UUID userId) {
        return messageService.getMessage(aiMessageId)
                .flatMap(aiMsg -> messageService.getMessage(aiMsg.getParentId()))
                .flatMapMany(userMsg -> {
                    StringBuilder aiResponseBuilder = new StringBuilder();
                    return chat(userMsg.getSessionId(), userId, userMsg.getContent(), userMsg.getParentId(), AiMode.NORMAL, null)
                            .doOnNext(aiResponseBuilder::append)
                            .doOnComplete(() -> {
                                messageService.saveMessage(userMsg.getSessionId(), userId, aiResponseBuilder.toString(), Sender.AGENT, userMsg.getId()).subscribe();
                            });
                });
    }
}
