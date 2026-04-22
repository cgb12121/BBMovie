package com.bbmovie.notificationservice.repository;

import com.bbmovie.notificationservice.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {
    List<UserDeviceToken> findByUserId(String userId);
}
