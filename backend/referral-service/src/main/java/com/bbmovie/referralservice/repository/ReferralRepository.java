package com.bbmovie.referralservice.repository;

import com.bbmovie.referralservice.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    Optional<Referral> findByReferredId(UUID referredId);
    boolean existsByReferredId(UUID referredId);
}
