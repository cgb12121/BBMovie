package com.example.bbmovie.repository;

import com.example.bbmovie.entity.jwt.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteAllByEmail(String email);

    Optional<RefreshToken> findByDeviceIpAddress(String deviceIpAddress);

    void deleteByEmailAndDeviceName(String email, String deviceName);

    Optional<RefreshToken> findByEmailAndDeviceName(String email, String deviceName);
}
