package com.bbmovie.promotionservice.repository;

import com.bbmovie.promotionservice.entity.UserPromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, UUID> {
    boolean existsByUserIdAndPromotionId(UUID userId, UUID promotionId);
}
