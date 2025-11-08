package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class _SetSessionArchivedDto {
    @NotNull
    private boolean isArchived;
}
