package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final MessageService messageService;
    private final ChatRequestFactory requestFactory;

    /**
     * Thực hiện chat với AI.
     * Logic xây dựng Request đã được tách ra ChatRequestFactory để đảm bảo Fail-Fast và Caching.
     */
    @Override
    public Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId, UUID assetId, AiMode mode, AiModel model) {
        log.info("[Chat] Session: {}, Mode: {}, Model: {}, Asset: {}", sessionId, mode, model, assetId);
        
        return requestFactory.createRequest(sessionId, userId, message, assetId, mode, model)
                .flatMapMany(spec -> spec.stream().content());
    }

    @Override
    public Flux<String> editMessage(UUID oldMessageId, UUID userId, String newContent) {
        return messageService.getMessage(oldMessageId)
                .flatMapMany(oldMsg -> chat(oldMsg.getSessionId(), userId, newContent, oldMsg.getParentId(), null, AiMode.NORMAL, null));
    }

    @Override
    public Flux<String> regenerateMessage(UUID aiMessageId, UUID userId) {
        return messageService.getMessage(aiMessageId)
                .flatMap(aiMsg -> messageService.getMessage(aiMsg.getParentId()))
                .flatMapMany(userMsg -> {
                    // Xóa log thủ công vì Advisor sẽ lo việc lưu trữ
                    return chat(userMsg.getSessionId(), userId, userMsg.getContent(), userMsg.getParentId(), null, AiMode.NORMAL, null);
                });
    }
}
