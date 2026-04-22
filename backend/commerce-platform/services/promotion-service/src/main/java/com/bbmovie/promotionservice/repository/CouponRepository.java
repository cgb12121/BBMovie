package com.bbmovie.promotionservice.repository;

import com.bbmovie.promotionservice.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCode(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Coupon c
            SET c.currentUsage = c.currentUsage + 1
            WHERE c.id = :couponId
              AND (c.usageLimit IS NULL OR c.currentUsage < c.usageLimit)
            """)
    int incrementUsage(@Param("couponId") UUID couponId);
}
