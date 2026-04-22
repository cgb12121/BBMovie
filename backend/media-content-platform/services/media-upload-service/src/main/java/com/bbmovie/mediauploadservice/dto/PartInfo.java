package com.bbmovie.mediauploadservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartInfo {
    @NotNull(message = "Part number is required")
    private Integer partNumber;

    @NotNull(message = "ETag is required")
    private String etag;
}

