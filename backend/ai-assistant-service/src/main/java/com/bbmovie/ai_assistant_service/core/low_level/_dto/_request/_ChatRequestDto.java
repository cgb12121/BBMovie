package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class _ChatRequestDto {
    @NotBlank
    private String message;
    @NotBlank
    private String assistantType; // e.g., "admin", "user"
}
