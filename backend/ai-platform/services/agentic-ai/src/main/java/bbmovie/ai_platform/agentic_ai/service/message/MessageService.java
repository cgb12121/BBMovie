package bbmovie.ai_platform.agentic_ai.service.message;

import bbmovie.ai_platform.agentic_ai.dto.response.ChatMessageResponse;
import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.aop_policy.annotation.CheckOwnership;

import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @CheckOwnership(expression = "#sessionId", entityType = "SESSION")
    public Mono<CursorPageResponse<ChatMessageResponse>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size) {
        Flux<ChatMessage> messageFlux;
        if (cursor == null || cursor.isBlank()) {
            messageFlux = messageRepository.findBySessionId(sessionId, userId, size + 1);
        } else {
            Instant cursorTime = Instant.parse(cursor);
            messageFlux = messageRepository.findBySessionIdWithCursor(sessionId, userId, cursorTime, size + 1);
        }

        return messageFlux.collectList().map(messages -> {
            boolean hasNext = messages.size() > size;
            List<ChatMessage> pagedMessages = hasNext ? messages.subList(0, size) : messages;
            String nextCursor = pagedMessages.isEmpty() ? null : pagedMessages.get(pagedMessages.size() - 1).getCreatedAt().toString();
            
            List<ChatMessageResponse> responses = pagedMessages.stream()
                    .map(m -> new ChatMessageResponse(m.getId(), m.getSessionId(), m.getContent(), m.getSenderType().name(), m.getParentId(), m.getCreatedAt()))
                    .toList();
            return new CursorPageResponse<>(responses, nextCursor, hasNext, size);
        });
    }

    public Mono<ChatMessage> saveMessage(UUID sessionId, UUID userId, String content, bbmovie.ai_platform.agentic_ai.entity.Sender sender, UUID parentId) {
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
    
    @CheckOwnership(expression = "#messageId", entityType = "MESSAGE")
    public Mono<ChatMessage> getMessage(UUID messageId) {
        return messageRepository.findById(messageId);
    }
}
