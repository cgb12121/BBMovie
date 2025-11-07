package com.bbmovie.ai_assistant_service.core.low_level._dto._request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class _DeleteArchivedSessionsDto {
    private List<UUID> sessionIds;
}
