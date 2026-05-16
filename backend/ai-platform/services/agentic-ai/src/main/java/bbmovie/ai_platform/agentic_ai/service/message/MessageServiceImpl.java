package bbmovie.ai_platform.agentic_ai.service.message;

import bbmovie.ai_platform.agentic_ai.dto.response.ChatMessageResponse;
import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.entity.enums.Sender;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.utils.CursorPageHelper;
import bbmovie.ai_platform.aop_policy.annotation.CheckOwnership;
import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    @CheckOwnership(expression = "#sessionId", entityType = "SESSION")
    public Mono<CursorPageResponse<ChatMessageResponse>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size) {
        Flux<ChatMessage> messageFlux = (cursor == null || cursor.isBlank())
                ? messageRepository.findBySessionId(sessionId, userId, size + 1)
                : messageRepository.findBySessionIdWithCursor(sessionId, userId, Instant.parse(cursor), size + 1);

        return messageFlux.collectList()
                .map(messages -> CursorPageHelper.toPage(messages, size,
                        m -> m.getCreatedAt().toString(),
                        m -> new ChatMessageResponse(m.getId(), m.getSessionId(), m.getContent(),
                                m.getSenderType().name(), m.getParentId(), m.getCreatedAt())));
    }

    @Override
    public Mono<ChatMessage> saveMessage(UUID sessionId, UUID userId, String content, Sender sender, UUID parentId) {
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .userId(userId)
                .content(content)
                .senderType(sender)
                .parentId(parentId)
                .build()
                .asNew();
        return messageRepository.save(message);
    }

    @Override
    @CheckOwnership(expression = "#messageId", entityType = "MESSAGE")
    public Mono<ChatMessage> getMessage(UUID messageId) {
        return messageRepository.findById(messageId);
    }
}
