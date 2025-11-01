package com.bbmovie.ai_assistant_service.core.low_level._dto;

import lombok.Data;

@Data
public class _ChatRequestDto {
    private String message;
    private String assistantType; // e.g., "admin", "user"
}
