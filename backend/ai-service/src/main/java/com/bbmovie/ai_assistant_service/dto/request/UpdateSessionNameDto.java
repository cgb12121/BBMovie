package com.bbmovie.ai_assistant_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSessionNameDto {
    @NotBlank
    private String newName;
}
