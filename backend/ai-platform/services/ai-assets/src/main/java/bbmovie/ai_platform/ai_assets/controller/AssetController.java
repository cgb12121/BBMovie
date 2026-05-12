package bbmovie.ai_platform.ai_assets.controller;

import bbmovie.ai_platform.ai_assets.entity.Asset;
import bbmovie.ai_platform.ai_assets.repository.AssetRepository;
import bbmovie.ai_platform.ai_assets.repository.IngestedContentRepository;
import bbmovie.ai_platform.ai_common.dto.IngestedContentDto;
import com.bbmovie.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetRepository assetRepository;
    private final IngestedContentRepository contentRepository;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Asset>> getAsset(@PathVariable UUID id) {
        return assetRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/content")
    public Mono<ResponseEntity<ApiResponse<IngestedContentDto>>> getAssetContent(@PathVariable UUID id) {
        return contentRepository.findByAssetId(id)
                .map(content -> {
                    IngestedContentDto dto = IngestedContentDto.builder()
                            .assetId(content.getAssetId())
                            .content(content.getContent())
                            .build();
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
