package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.ToolManager;
import bbmovie.ai_platform.agentic_ai.service.personalize.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Factory responsible for building complex ChatClient requests.
 * Handles mode-specific options, model capability validation, tool injection, and personalization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRequestFactory {

    private final PersonalizationService personalizationService;
    private final ToolManager toolManager; 

    /**
     * Creates a fully configured ChatClientRequestSpec.
     * Performs "Fail Fast" validation on model capabilities and injects personalized context.
     */
    public Mono<ChatClient.ChatClientRequestSpec> createRequest(
            ChatClient chatClient, UUID sessionId, UUID userId,
            String message, AiMode mode, AiModel model) {

        // 1. Validate Model
        if (model == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No AI model specified."));
        }
        
        log.debug("Building request for Session: {}, Mode: {}, Model: {}", sessionId, mode, model.name());

        // 2. Fail Fast: Validate Thinking capability
        if (mode == AiMode.THINKING && !model.isSupportsThinking()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model " + model.name() + " does not support Thinking mode."));
        }

        // 3. Build Model Options
        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                .model(model.getValue());

        // 4. Set Mode-specific configurations
        switch (mode) {
            case THINKING -> optionsBuilder.thinkOption(ThinkOption.ThinkBoolean.ENABLED).temperature(0.3);
            case NORMAL -> optionsBuilder.thinkOption(ThinkOption.ThinkBoolean.DISABLED).temperature(0.7);
            default -> {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported AI mode."));
            }
        }

        // 5. Fetch Personalized Context (L1-L2-L3)
        return personalizationService.getPersonalizedContext(userId, sessionId)
                .map(personaBrief -> {
                    ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                            // Injects parameters into the DEFAULT system prompt defined in ChatClientConfig
                            .system(s -> s
                                    .param("user_context", personaBrief)
                                    .param("current_date", OffsetDateTime.now())
                                    .param("user_id", userId.toString())
                            )
                            .user(message)
                            .options(optionsBuilder)
                            .advisors(a -> a
                                    .param("chat_memory_conversation_id", userId.toString() + ":" + sessionId.toString())
                                    .param("chat_memory_retrieve_size", 20)
                            )
                            .toolContext(Map.of("userId", userId));

                    // 6. Conditional Tool Injection
                    if (model.isSupportsTools()) {
                        spec.tools(toolManager.getAllTools());
                    }
                    
                    return spec;
                });
    }
}
