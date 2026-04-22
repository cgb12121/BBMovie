package bbmovie.commerce.entitlement_service.infrastructure.persistence.repo;

import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.PlanContentPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanContentPolicyRepository extends JpaRepository<PlanContentPolicyEntity, Long> {
    List<PlanContentPolicyEntity> findByPlanIdAndEnabledTrue(String planId);
}
