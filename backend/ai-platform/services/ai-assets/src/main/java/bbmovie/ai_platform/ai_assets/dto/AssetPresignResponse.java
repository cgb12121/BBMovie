package bbmovie.ai_platform.ai_assets.dto;

import java.util.UUID;

public record AssetPresignResponse(
    UUID assetId,
    String uploadUrl
) {}
