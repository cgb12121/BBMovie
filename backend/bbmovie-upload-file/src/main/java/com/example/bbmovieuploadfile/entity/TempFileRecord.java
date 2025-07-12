package com.example.bbmovieuploadfile.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("temp_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempFileRecord {
    @Id
    @Column("id")
    private String id;

    @Column("file_name")
    private String fileName;

    @Column("extension")
    private String extension;

    @Column("size")
    private long size;

    @Column("temp_dir")
    private String tempDir;

    @Column("temp_store_for")
    private String tempStoreFor;

    @Column("uploaded_by")
    private String uploadedBy;

    @Column("is_removed")
    private boolean isRemoved;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("removed_at")
    private LocalDateTime removedAt;
}