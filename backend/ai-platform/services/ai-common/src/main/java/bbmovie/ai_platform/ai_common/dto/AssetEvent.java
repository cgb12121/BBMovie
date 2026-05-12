package bbmovie.ai_platform.ai_common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import bbmovie.ai_platform.ai_common.enums.AssetStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetEvent {
    private UUID assetId;
    private UUID userId;
    private String bucket;
    private String objectKey;
    private AssetStatus status;
    private String content; 
}
