package com.bbmovie.payment.repository;

import com.bbmovie.payment.entity.DiscountCampaign;
import com.bbmovie.payment.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountCampaignRepository extends JpaRepository<DiscountCampaign, UUID> {
    List<DiscountCampaign> findByPlanAndActiveIsTrue(SubscriptionPlan plan);
    List<DiscountCampaign> findByPlanAndActiveIsTrueAndStartAtBeforeAndEndAtAfter(SubscriptionPlan plan, LocalDateTime now1, LocalDateTime now2);
}


