package com.example.bbmovie.repository;

import com.example.bbmovie.model.UserSubscription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends BaseRepository<UserSubscription> {
    Optional<UserSubscription> findByUserIdAndIsActiveTrue(Long userId);
    
    List<UserSubscription> findByEndDateBeforeAndIsActiveTrue(LocalDateTime date);
    
    List<UserSubscription> findByAutoRenewTrueAndEndDateBefore(LocalDateTime date);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.isActive = true AND us.endDate > CURRENT_TIMESTAMP")
    Optional<UserSubscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.paymentGatewayId = :paymentGatewayId")
    Optional<UserSubscription> findByPaymentGatewayId(@Param("paymentGatewayId") String paymentGatewayId);
} 