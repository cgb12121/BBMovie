package bbmovie.ai_platform.agentic_ai.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ModelRoutingService is a dynamic router that selects the appropriate Spring AI {@link ChatModel}
 * based on the requested provider.
 * 
 * It manages the availability of different AI providers (Ollama, Google, OpenAI) and ensures
 * that a runtime exception is thrown if a requested provider is not configured in the application.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRoutingService {

    private final Optional<OllamaChatModel> ollamaChatModel;
    private final Optional<GoogleGenAiChatModel> googleChatModel;
    private final Optional<OpenAiChatModel> openAiChatModel;

    /**
     * Retrieves the ChatModel instance for the given provider name.
     * 
     * @param provider The name of the AI provider (e.g., "ollama", "google", "openai").
     * @return The corresponding {@link ChatModel} bean.
     * @throws RuntimeException if the provider is not configured.
     * @throws IllegalArgumentException if the provider name is unknown.
     */
    public ChatModel getModel(String provider) {
        log.info("[ModelRouting] Selecting model for provider: {}", provider);
        
        return switch (provider.toLowerCase()) {
            case "ollama" -> ollamaChatModel.orElseThrow(() -> new RuntimeException("Ollama model not configured"));
            case "gemini", "google" -> googleChatModel.orElseThrow(() -> new RuntimeException("Gemini model not configured"));
            case "groq", "openai" -> openAiChatModel.orElseThrow(() -> new RuntimeException("Groq/OpenAI model not configured"));
            default -> throw new IllegalArgumentException("Unknown model provider: " + provider);
        };
    }
}
