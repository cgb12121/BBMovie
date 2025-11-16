package com.bbmovie.ai_assistant_service.dto.request;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequestDto {
    @NotBlank
    private String message;
    @NotBlank
    private String assistantType; // e.g., "admin", "user"
    @NotNull
    private AiMode aiMode;
}
