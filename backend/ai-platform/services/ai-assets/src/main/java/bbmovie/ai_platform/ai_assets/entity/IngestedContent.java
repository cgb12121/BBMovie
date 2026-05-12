package bbmovie.ai_platform.ai_assets.entity;

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
@Table("ai_ingested_content")
public class IngestedContent {

    @Id
    private UUID id;

    @Column("asset_id")
    private UUID assetId;

    @Column("content")
    private String content;

    @Column("word_count")
    private Integer wordCount;

    @Column("created_at")
    private LocalDateTime createdAt;
}
