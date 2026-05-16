package bbmovie.ai_platform.agentic_ai.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateAgentConfigDto(
        @Size(max = 2000, message = "Custom instructions must not exceed 2000 characters")
        String customInstructions,

        @Size(max = 50, message = "Tone must not exceed 50 characters")
        String tone
) {}
