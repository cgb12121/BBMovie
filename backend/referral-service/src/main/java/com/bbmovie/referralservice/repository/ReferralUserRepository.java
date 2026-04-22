package com.bbmovie.referralservice.repository;

import com.bbmovie.referralservice.entity.ReferralUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralUserRepository extends JpaRepository<ReferralUser, UUID> {
    Optional<ReferralUser> findByUserId(UUID userId);
    Optional<ReferralUser> findByReferralCode(String referralCode);
}
