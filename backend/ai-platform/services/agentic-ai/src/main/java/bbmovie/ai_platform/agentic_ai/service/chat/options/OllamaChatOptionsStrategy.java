package bbmovie.ai_platform.agentic_ai.service.chat.options;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.stereotype.Component;

/**
 * Strategy for Ollama-specific chat options, including "thinking" mode support.
 */
@Component
public class OllamaChatOptionsStrategy implements ChatOptionsStrategy {
    
    @Override
    public boolean supports(String provider) {
        return "ollama".equalsIgnoreCase(provider);
    }

    @Override
    public ChatOptions.Builder<?> buildOptions(AiModel model, AiMode mode) {
        OllamaChatOptions.Builder builder = OllamaChatOptions.builder().model(model.getModel());
        
        if (mode == AiMode.THINKING) {
            builder.thinkOption(ThinkOption.ThinkBoolean.ENABLED)
                   .temperature(0.3);
        } else {
            builder.thinkOption(ThinkOption.ThinkBoolean.DISABLED)
                   .temperature(0.7);
        }
        
        return builder;
    }
}
