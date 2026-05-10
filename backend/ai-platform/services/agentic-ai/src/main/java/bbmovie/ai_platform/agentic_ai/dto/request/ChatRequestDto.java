package bbmovie.ai_platform.agentic_ai.dto.request;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;

public record ChatRequestDto(@NotNull String message, @Nullable UUID parentId) {}

