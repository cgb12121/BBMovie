package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class _CreateSessionDto {
    @NotBlank
    private String sessionName;
}
