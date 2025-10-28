package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.config.llm.memory.PersistentChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatService {

    private final ChatMemoryProvider chatMemoryProvider;

    public ChatService(ChatMemoryProvider chatMemoryProvider) {
        this.chatMemoryProvider = chatMemoryProvider;
    }

    public Mono<Void> clearUserChat(String userId) {
        return Mono.fromRunnable(() -> {
            try {
                PersistentChatMemoryStore.enableDelete();
                ChatMemory memory = chatMemoryProvider.get(userId);
                memory.clear(); // will now call deleteMessages()
            } finally {
                PersistentChatMemoryStore.disableDelete();
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
