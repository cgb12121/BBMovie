package bbmovie.ai_platform.agentic_ai.exception;

/**
 * Thrown when a requested AI provider (e.g., Ollama, Gemini, OpenAI) is not
 * configured or available in the current Spring application context.
 *
 * <p>This is a distinct exception from {@link IllegalArgumentException} (unknown provider)
 * and allows the {@link GlobalExceptionHandler} to return a meaningful HTTP 503
 * instead of a generic 500.
 */
public class ProviderNotConfiguredException extends RuntimeException {

    private final String provider;

    public ProviderNotConfiguredException(String provider) {
        super("AI provider '" + provider + "' is not configured. "
                + "Check that the required Spring AI starter and application properties are present.");
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
