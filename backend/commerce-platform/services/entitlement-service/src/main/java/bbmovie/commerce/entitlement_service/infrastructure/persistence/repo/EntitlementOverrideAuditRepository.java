package bbmovie.commerce.entitlement_service.infrastructure.persistence.repo;

import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementOverrideAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntitlementOverrideAuditRepository extends JpaRepository<EntitlementOverrideAuditEntity, Long> {
}
