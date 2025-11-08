package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class _ChatRequestDto {
    @NotBlank
    private String message;
    @NotBlank
    private String assistantType; // e.g., "admin", "user"
    @NotNull
    private _AiMode aiMode;
}
