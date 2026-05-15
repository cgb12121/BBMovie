package bbmovie.ai_platform.agentic_ai.service.chat.options;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Strategy for OpenAI and Groq-compatible chat options.
 */
@Component
public class OpenAiChatOptionsStrategy implements ChatOptionsStrategy {

    @Override
    public boolean supports(String provider) {
        return "openai".equalsIgnoreCase(provider) || "groq".equalsIgnoreCase(provider);
    }

    @Override
    public ChatOptions.Builder<?> buildOptions(AiModel model, AiMode mode) {
        return OpenAiChatOptions.builder()
                .model(model.getModel())
                .temperature(mode == AiMode.THINKING ? 0.3 : 0.7);
    }
}
