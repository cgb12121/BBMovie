package bbmovie.ai_platform.agentic_ai.dto.request;

import java.util.UUID;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;

public record ChatRequestDto(
    @NotNull String message, 
    @Nullable UUID parentId,
    @Nullable AiMode aiMode,
    @Nullable AiModel model
) {}
