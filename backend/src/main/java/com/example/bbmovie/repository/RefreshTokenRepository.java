package com.example.bbmovie.repository;

import com.example.bbmovie.entity.jwt.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByEmail(String email);

    default void save(String token, String email, Date expiryDate) {
        RefreshToken refreshToken = new RefreshToken(token, email, expiryDate);
        save(refreshToken);
    }

    Optional<RefreshToken> findByEmail(String email);
}
