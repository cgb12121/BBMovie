package com.bbmovie.common.dtos.nats;

import com.bbmovie.common.enums.EntityType;
import com.bbmovie.common.enums.Storage;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {
    @NotNull(message = "Id is required")
    private String fileId;
    @NotNull(message = "entityType is required")
    private EntityType fileType;
    @NotNull(message = "storage is required")
    private Storage storage;
    @Nullable
    String quality;
}