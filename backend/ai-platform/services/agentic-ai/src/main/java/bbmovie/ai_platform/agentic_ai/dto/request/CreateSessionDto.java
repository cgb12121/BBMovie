package bbmovie.ai_platform.agentic_ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionDto(
        @NotBlank(message = "Session name must not be blank")
        @Size(max = 100, message = "Session name must not exceed 100 characters")
        String sessionName
) {}
