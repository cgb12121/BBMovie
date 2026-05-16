package bbmovie.ai_platform.agentic_ai.config;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for AI streaming timeouts.
 *
 * <p>Thinking mode requires a significantly longer timeout because the model
 * performs explicit multi-step reasoning before generating the response.
 * Standard chat has a shorter deadline to avoid dangling SSE connections.
 *
 * <p>Configure in {@code application.properties}:
 * <pre>
 * ai.timeout.default-timeout=120s
 * ai.timeout.thinking-timeout=300s
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.timeout")
public class AiTimeoutProperties {

    /** Timeout for standard chat and regeneration requests. Default: 2 minutes. */
    private Duration defaultTimeout = Duration.ofMinutes(2);

    /** Timeout for THINKING mode requests (extended reasoning). Default: 5 minutes. */
    private Duration thinkingTimeout = Duration.ofMinutes(5);

    /**
     * Returns the appropriate timeout duration for the given AI mode.
     * Returns {@link #defaultTimeout} for {@code null} mode.
     */
    public Duration getTimeoutForMode(AiMode mode) {
        return mode == AiMode.THINKING ? thinkingTimeout : defaultTimeout;
    }
}
