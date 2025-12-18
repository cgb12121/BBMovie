package com.bbmovie.mediauploadservice.entity;

import com.bbmovie.mediauploadservice.enums.VariantFormat;
import com.bbmovie.mediauploadservice.enums.VideoResolution;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "media_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @Enumerated(EnumType.STRING)
    private VideoResolution resolution;

    @Enumerated(EnumType.STRING)
    private VariantFormat format;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    @Column(name = "encryption_key_path")
    private String encryptionKeyPath;

    private Integer bitrate;

    private Long sizeBytes;
}
