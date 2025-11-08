package com.bbmovie.ai_assistant_service.core.low_level._entity._model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class _AssistantMetadata {
    private _AssistantType type;
    private String modelName;
    private String description;
    private List<String> capabilities;
}
