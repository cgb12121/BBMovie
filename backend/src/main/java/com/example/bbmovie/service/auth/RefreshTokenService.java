package com.example.bbmovie.service.auth;

import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.repository.RefreshTokenRepository;
import com.example.bbmovie.security.jwt.asymmetric.JwtTokenPairedKeyProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtTokenPairedKeyProvider jwtTokenPairedKeyProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void deleteByEmail(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenPairedKeyProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtTokenPairedKeyProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (tokenEntity.isRevoked() || tokenEntity.getExpiryDate().before(new Date())) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        return jwtTokenPairedKeyProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> refreshToken.setRevoked(true));
    }

    @Transactional
    public void saveRefreshToken(String token, String email) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmail(email);
        Date expiryDate = jwtTokenPairedKeyProvider.getExpirationDateFromToken(token);

        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get();
            refreshToken.setToken(token);
            refreshToken.setExpiryDate(expiryDate);
            refreshToken.setRevoked(false);
        } else {
            refreshToken = new RefreshToken(token, email, expiryDate);
        }

        refreshTokenRepository.save(refreshToken);
    }
}
