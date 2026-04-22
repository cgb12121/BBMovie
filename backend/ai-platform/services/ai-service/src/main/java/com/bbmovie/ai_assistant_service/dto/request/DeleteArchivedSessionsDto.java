package com.bbmovie.ai_assistant_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeleteArchivedSessionsDto {
    @NotNull @NotEmpty
    private List<UUID> sessionIds;
}
