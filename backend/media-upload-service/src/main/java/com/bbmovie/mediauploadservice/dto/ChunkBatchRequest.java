package com.bbmovie.mediauploadservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkBatchRequest {
    @NotNull(message = "From part number is required")
    @Positive(message = "From part number must be positive")
    private Integer from;

    @NotNull(message = "To part number is required")
    @Positive(message = "To part number must be positive")
    private Integer to;
}

