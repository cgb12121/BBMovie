package bbmovie.ai_platform.agentic_ai.service.chat.options;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Strategy for Google Gemini-specific chat options.
 */
@Component
public class GoogleChatOptionsStrategy implements ChatOptionsStrategy {

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider) || "gemini".equalsIgnoreCase(provider);
    }

    @Override
    public ChatOptions.Builder<?> buildOptions(AiModel model, AiMode mode) {
        return GoogleGenAiChatOptions.builder()
                .model(model.getModel())
                .temperature(mode == AiMode.THINKING ? 0.3 : 0.7);
    }
}
