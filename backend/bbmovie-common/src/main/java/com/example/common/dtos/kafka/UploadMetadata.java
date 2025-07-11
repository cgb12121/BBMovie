package com.example.common.dtos.kafka;

import com.example.common.enums.EntityType;
import com.example.common.enums.Storage;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {
    @NotNull(message = "entityType is required")
    private EntityType entityType;
    @NotNull(message = "storage is required")
    private Storage storage;
    @Nullable
    List<String> quality;
}