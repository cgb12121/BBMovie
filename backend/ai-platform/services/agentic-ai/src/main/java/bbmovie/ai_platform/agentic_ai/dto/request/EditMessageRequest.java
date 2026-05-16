package bbmovie.ai_platform.agentic_ai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequest(
        @NotBlank(message = "New content must not be blank")
        String newContent
) {}
