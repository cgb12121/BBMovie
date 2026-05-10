package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.agentic_ai.entity.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatClient chatClient;
    private final MessageService messageService;
    private final org.springframework.ai.chat.memory.ChatMemory chatMemory;

    public ChatService(ChatClient.Builder chatClientBuilder, MessageService messageService, org.springframework.ai.chat.memory.ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.messageService = messageService;
        this.chatMemory = chatMemory;
    }

    public Flux<String> chat(UUID sessionId, UUID userId, String message, UUID parentId) {
        log.info("[Chat] Session: {}, Message: {}", sessionId, message);
        
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", sessionId.toString())
                        .param("chat_memory_retrieve_size", 20)
                )
                .toolContext(Map.of("userId", userId))
                .toolNames("saveMemory", "updateMemory")
                .stream()
                .content();
    }

    public Flux<String> editMessage(UUID oldMessageId, UUID userId, String newContent) {
        return messageService.getMessage(oldMessageId)
                .flatMapMany(oldMsg -> chat(oldMsg.getSessionId(), userId, newContent, oldMsg.getParentId()));
    }

    public Flux<String> regenerateMessage(UUID aiMessageId, UUID userId) {
        return messageService.getMessage(aiMessageId)
                .flatMap(aiMsg -> messageService.getMessage(aiMsg.getParentId()))
                .flatMapMany(userMsg -> {
                    StringBuilder aiResponseBuilder = new StringBuilder();
                    return chatClient.prompt()
                            .user(userMsg.getContent())
                            .toolContext(Map.of("userId", userId))
                            .toolNames("saveMemory", "updateMemory")
                            .stream()
                            .content()
                            .doOnNext(aiResponseBuilder::append)
                            .doOnComplete(() -> {
                                messageService.saveMessage(userMsg.getSessionId(), userId, aiResponseBuilder.toString(), Sender.AGENT, userMsg.getId()).subscribe();
                            });
                });
    }
}
