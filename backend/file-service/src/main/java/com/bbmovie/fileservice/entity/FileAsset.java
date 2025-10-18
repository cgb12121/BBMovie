package com.bbmovie.fileservice.entity;

import com.example.common.enums.EntityType;
import com.example.common.enums.Storage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("file_assets")
public class FileAsset {

    @Id
    private Long id;

    private String movieId;

    private EntityType entityType;

    private Storage storageProvider;

    private String pathOrPublicId;

    private String quality;

    private String mimeType;

    private Long fileSize;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
