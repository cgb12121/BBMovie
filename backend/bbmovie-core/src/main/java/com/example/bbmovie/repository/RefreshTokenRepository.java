package com.example.bbmovie.repository;

import com.example.bbmovie.entity.jose.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteAllByEmail(String email);

    void deleteByEmailAndDeviceName(String email, String deviceName);

    Optional<RefreshToken> findByEmailAndDeviceName(String email, String deviceName);

    Optional<RefreshToken> findAllByEmail(String email);

    @Query("SELECT r FROM RefreshToken r WHERE r.email = :email AND r.revoked = false AND r.expiryDate > CURRENT_TIMESTAMP")
    List<RefreshToken> findAllValidByEmail(@Param("email") String email);
}
