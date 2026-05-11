package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.ToolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

/**
 * Factory responsible for building complex ChatClient requests.
 * Handles mode-specific options, model capability validation, and tool injection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRequestFactory {

    private final ToolManager toolManager;

    /**
     * Creates a fully configured ChatClientRequestSpec.
     * Performs "Fail Fast" validation on model capabilities.
     */
    public ChatClient.ChatClientRequestSpec createRequest(
            ChatClient chatClient, UUID sessionId, UUID userId,
            String message, AiMode mode, AiModel model) {

        // 1. Throw error if model is null (should be at validation)
        if (model == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model does not support Thinking (Reasoning) mode.");
        }
        AiModel activeModel =  model;
        
        log.debug("Building request for Session: {}, Mode: {}, Model: {}", sessionId, mode, activeModel.name());

        // 2. Fail Fast: Validate Thinking capability
        if (mode == AiMode.THINKING && !activeModel.isSupportsThinking()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model " + activeModel.name() + " does not support Thinking (Reasoning) mode.");
        }

        // 3. Build Model Options
        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                .model(activeModel.getValue());

        // 4. Set Mode-specific configurations
        switch (mode) {
            case THINKING:
                optionsBuilder.thinkOption(ThinkOption.ThinkBoolean.ENABLED)
                              .temperature(0.3); // Lower temp for reasoning stability
                break;
            case NORMAL:
                optionsBuilder.thinkOption(ThinkOption.ThinkBoolean.DISABLED)
                              .temperature(0.7);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model " + activeModel.name() + " does not support Thinking (Reasoning) mode.");
        }

        // 5. Initialize Request Spec with History Instructions
        String historyInstruction = "You have access to the conversation history via the provided context. Use it to provide relevant and coherent responses.";
        
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(s -> s.text("You are a helpful AI assistant. " + historyInstruction))
                .user(message)
                .options(optionsBuilder)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", userId.toString() + ":" + sessionId.toString())
                        .param("chat_memory_retrieve_size", 20)
                )
                .toolContext(Map.of("userId", userId));

        // 6. Conditional Tool Injection: Only if model supports it
        if (activeModel.isSupportsTools()) {
            spec.tools(toolManager.getAllTools());
        } else {
            log.warn("Model {} does not support tools. Skipping tool injection.", activeModel.name());
        }

        return spec;
    }
}
