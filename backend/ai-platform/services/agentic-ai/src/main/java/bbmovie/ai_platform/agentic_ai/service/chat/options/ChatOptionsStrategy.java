package bbmovie.ai_platform.agentic_ai.service.chat.options;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Strategy interface for building provider-specific ChatOptions.
 */
public interface ChatOptionsStrategy {
    
    /**
     * Checks if this strategy supports the given provider.
     */
    boolean supports(String provider);

    /**
     * Builds the ChatOptions.Builder for the specific provider.
     */
    ChatOptions.Builder<?> buildOptions(AiModel model, AiMode mode);
}
