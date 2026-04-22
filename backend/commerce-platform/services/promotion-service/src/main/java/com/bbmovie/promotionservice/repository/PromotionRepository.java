package com.bbmovie.promotionservice.repository;

import com.bbmovie.promotionservice.entity.Promotion;
import com.bbmovie.promotionservice.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    List<Promotion> findByStatusAndAutomaticTrue(PromotionStatus status);

    List<Promotion> findByStatusAndAutomaticTrueAndStartDateBeforeAndEndDateAfter(
            PromotionStatus status, LocalDateTime start, LocalDateTime end);
}
