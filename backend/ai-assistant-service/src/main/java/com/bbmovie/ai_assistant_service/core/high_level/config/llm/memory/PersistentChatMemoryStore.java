package com.bbmovie.ai_assistant_service.core.high_level.config.llm.memory;

import com.bbmovie.ai_assistant_service.core.high_level.agent.domain.entity.ChatMessageEntity;
import com.bbmovie.ai_assistant_service.core.high_level.repository.ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.high_level.repository.ChatSessionRepository;
import com.bbmovie.ai_assistant_service.core.high_level.utils.LangchainMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bbmovie.ai_assistant_service.core.high_level.utils.LangchainMapper.extractMessageContent;
import static com.bbmovie.ai_assistant_service.core.high_level.utils.LangchainMapper.toChatMessageEntity;

@Slf4j
@Component("persistentChatMemoryStore")
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
        try {
            String sessionId = (String) memoryId;
            return chatSessionRepository.findById(sessionId)
                    .flatMapMany(session ->
                            chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                    )
                    .map(LangchainMapper::toLangChain4jChatMessage)
                    .collectList()
                    .toFuture()
                    .join();
        } catch (Exception e) {
            log.error("Failed to get messages for sessionId={}", memoryId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = (String) memoryId;
        chatSessionRepository.findById(sessionId)
                .flatMap(session -> chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                        .collectList()
                        .flatMap(existing -> {

                            Set<String> existingKeys = existing.stream()
                                    .map(e -> e.getRole() + "|" + e.getContent())
                                    .collect(Collectors.toSet());

                            List<ChatMessageEntity> newEntities = messages.stream()
                                    .filter(msg -> {
                                        String role = (msg instanceof UserMessage) ? "USER" : "AI";
                                        String content = extractMessageContent(msg);
                                        String key = role + "|" + content;
                                        return !existingKeys.contains(key);
                                    })
                                    .map(msg -> toChatMessageEntity(msg, session.getId()))
                                    .toList();

                            if (newEntities.isEmpty()) {
                                log.debug("No new chat messages for sessionId={}", sessionId);
                                return Mono.empty();
                            }

                            return chatMessageRepository.saveAll(newEntities)
                                    .then(Mono.fromRunnable(() ->
                                            log.debug("Inserted {} new messages for sessionId={}", newEntities.size(), sessionId)
                                    ));
                        })
                )
                .doOnError(error -> log.error("Failed to update chat messages for sessionId={}", sessionId, error))
                .subscribe();
    }


    @Override
    public void deleteMessages(Object memoryId) {
        if (!allowDelete.get()) {
            log.warn("Prevented deletion of ChatMemory for sessionId={} (unauthorized call)", memoryId);
            log.warn("This behavior might be trigger by langchain4j");
            return;
        }

        String sessionId = (String) memoryId;
        chatMessageRepository.deleteBySessionId(sessionId)
                .then(chatSessionRepository.deleteById(sessionId))
                .doOnSuccess(unused -> log.debug("Deleted chat session and messages for sessionId={}", sessionId))
                .doOnError(error -> log.error("Failed to delete messages for sessionId={}", sessionId, error))
                .subscribe();
    }

    public static void enableDelete() {
        allowDelete.set(true);
    }

    public static void disableDelete() {
        allowDelete.set(false);
    }
}
