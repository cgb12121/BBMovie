package bbmovie.ai_platform.ai_assests.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_assets")
public class Asset {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("bucket_name")
    private String bucketName;

    @Column("object_key")
    private String objectKey;

    @Column("content_type")
    private String contentType;

    @Column("size")
    private Long size;

    @Column("status")
    private AssetStatus status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
