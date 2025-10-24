package com.bbmovie.ai_assistant_service.config.memory;

import com.bbmovie.ai_assistant_service.domain.entity.ChatMessageEntity;
import com.bbmovie.ai_assistant_service.domain.entity.ChatSession;
import com.bbmovie.ai_assistant_service.repository.ChatMessageRepository;
import com.bbmovie.ai_assistant_service.repository.ChatSessionRepository;
import com.bbmovie.ai_assistant_service.utils.LangchainMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.bbmovie.ai_assistant_service.utils.LangchainMapper.toChatMessageEntity;

@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    // flag to control who can delete. Langchain should not wipe out a user's conservation history
    private static final ThreadLocal<Boolean> allowDelete = ThreadLocal.withInitial(() -> false);

    @Autowired
    public PersistentChatMemoryStore(ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // Fire the reactive chain synchronously via .toFuture().join() to avoid blocking the reactor thread
        try {
            return chatSessionRepository.findByUserId(memoryId.toString())
                    .flatMapMany(session ->
                            chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                    )
                    .map(LangchainMapper::toLangChain4jChatMessage)
                    .collectList()
                    .toFuture()
                    .join(); // safe since this runs on boundedElastic implicitly from the repository layer
        } catch (Exception e) {
            log.error("Failed to get messages for memoryId={}", memoryId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        chatSessionRepository.findByUserId(memoryId.toString())
                .switchIfEmpty(chatSessionRepository.save(ChatSession.builder()
                        .userId(memoryId.toString())
                        .sessionName("Chat Session for " + memoryId)
                        .createdAt(LocalDateTime.now())
                        .build())
                )
                .flatMap(session -> {
                    List<ChatMessageEntity> entities = messages.stream()
                            .map(msg -> toChatMessageEntity(msg, session.getId()))
                            .toList();

                    return chatMessageRepository.saveAll(entities).then();
                })
                .doOnSuccess(unused -> log.debug("Chat messages updated for {}", memoryId))
                .doOnError(error -> log.error("Failed to update chat messages for {}", memoryId, error))
                .subscribe();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        if (!allowDelete.get()) {
            log.warn("Prevented deletion of ChatMemory for memoryId={} (unauthorized call)", memoryId);
            log.warn("This behavior might be trigger by langchain4j");
            return;
        }

        chatSessionRepository.findByUserId(memoryId.toString())
                .flatMap(session ->
                        chatMessageRepository.deleteBySessionId(session.getId())
                                .then(chatSessionRepository.deleteById(session.getId()))
                )
                .doOnSuccess(unused -> log.debug("Deleted messages for {}", memoryId))
                .doOnError(error -> log.error("Failed to delete messages for {}", memoryId, error))
                .subscribe();
    }

    public static void enableDelete() {
        allowDelete.set(true);
    }

    public static void disableDelete() {
        allowDelete.set(false);
    }
}
