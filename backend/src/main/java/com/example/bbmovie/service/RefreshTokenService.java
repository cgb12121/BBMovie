package com.example.bbmovie.service;

import com.example.bbmovie.model.jwt.RefreshToken;
import com.example.bbmovie.repository.RefreshTokenRepository;
import com.example.bbmovie.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (tokenEntity.isRevoked() || tokenEntity.getExpiryDate().before(new Date())) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        return jwtTokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    public void deleteByEmail(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
