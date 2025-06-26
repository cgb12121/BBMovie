package com.example.bbmovie.repository;

import com.example.bbmovie.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends BaseRepository<SubscriptionPlan> {
    Optional<SubscriptionPlan> findByName(String name);

    @Transactional
    @Modifying
    @Query("update SubscriptionPlan s set s.active = ?1 where s.id = ?2")
    void updateActiveById(Long id);
}