package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class _DeleteArchivedSessionsDto {
    @NotNull @NotEmpty
    private List<UUID> sessionIds;
}
