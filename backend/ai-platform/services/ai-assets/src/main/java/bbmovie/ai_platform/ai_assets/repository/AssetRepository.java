package bbmovie.ai_platform.ai_assets.repository;

import bbmovie.ai_platform.ai_assets.entity.Asset;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AssetRepository extends R2dbcRepository<Asset, UUID> {
}
