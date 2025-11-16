package com.bbmovie.ai_assistant_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetSessionArchivedDto {
    @NotNull
    private boolean isArchived;
}
