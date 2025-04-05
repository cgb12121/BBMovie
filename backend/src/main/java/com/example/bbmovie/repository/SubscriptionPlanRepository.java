package com.example.bbmovie.repository;

import com.example.bbmovie.model.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends BaseRepository<SubscriptionPlan> {
    Page<SubscriptionPlan> findAllByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true AND sp.pricePerMonth <= :maxPrice")
    List<SubscriptionPlan> findActivePlansByMaxPrice(@Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true ORDER BY sp.pricePerMonth ASC")
    List<SubscriptionPlan> findAllActivePlansOrderByPrice();
} 