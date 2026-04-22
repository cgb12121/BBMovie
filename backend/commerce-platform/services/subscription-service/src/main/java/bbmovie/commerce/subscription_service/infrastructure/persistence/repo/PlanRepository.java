package bbmovie.commerce.subscription_service.infrastructure.persistence.repo;

import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, Long> {
    Optional<PlanEntity> findByPlanId(String planId);
}
