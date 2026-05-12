package bbmovie.ai_platform.agentic_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRoutingService {

    private final Optional<OllamaChatModel> ollamaChatModel;
    private final Optional<GoogleGenAiChatModel> googleChatModel;
    private final Optional<OpenAiChatModel> openAiChatModel;

    public ChatModel getModel(String provider) {
        log.info("[ModelRouting] Selecting model for provider: {}", provider);
        
        return switch (provider.toLowerCase()) {
            case "ollama" -> ollamaChatModel.orElseThrow(() -> new RuntimeException("Ollama model not configured"));
            case "gemini", "google" -> googleChatModel.orElseThrow(() -> new RuntimeException("Gemini model not configured"));
            case "groq", "openai" -> openAiChatModel.orElseThrow(() -> new RuntimeException("Groq/OpenAI model not configured"));
            default -> throw new IllegalArgumentException("Unknown model");
        };
    }
}
