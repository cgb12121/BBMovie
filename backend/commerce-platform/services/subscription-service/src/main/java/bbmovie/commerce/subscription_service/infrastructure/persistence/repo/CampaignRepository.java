package bbmovie.commerce.subscription_service.infrastructure.persistence.repo;

import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {
    Optional<CampaignEntity> findByCampaignId(String campaignId);
}
