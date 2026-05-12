package bbmovie.ai_platform.ai_assets.repository;

import bbmovie.ai_platform.ai_assets.entity.IngestedContent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface IngestedContentRepository extends R2dbcRepository<IngestedContent, UUID> {
    Mono<IngestedContent> findByAssetId(UUID assetId);
}
