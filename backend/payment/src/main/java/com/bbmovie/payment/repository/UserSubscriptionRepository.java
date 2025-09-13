package com.bbmovie.payment.repository;

import com.bbmovie.payment.entity.UserSubscription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    List<UserSubscription> findByUserId(String userId);
    long countByIsActiveTrue();
    long countByAutoRenewTrueAndIsActiveTrue();

    @Query("select count(u) from UserSubscription u where u.isActive = true and u.endDate < ?1")
    long countActiveButPastEnd(LocalDateTime now);
}


