package com.example.bbmovie.service.auth;

import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.exception.NoRefreshTokenException;
import com.example.bbmovie.repository.RefreshTokenRepository;
import com.example.bbmovie.security.jwt.JwtProviderStrategyContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtProviderStrategyContext jwtProviderStrategyContext;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void deleteByEmailAndDeviceName(String email, String deviceName) {
        refreshTokenRepository.deleteByEmailAndDeviceName(email, deviceName);
    }

    @Transactional
    public void deleteAllRefreshTokenByEmail(String email) {
        refreshTokenRepository.deleteAllByEmail(email);
    }

    public String refreshAccessToken(String oldAccessToken, String deviceName) {
        String username = jwtProviderStrategyContext.get().getUsernameFromToken(oldAccessToken);
        List<String> roles = jwtProviderStrategyContext.get().getRolesFromToken(oldAccessToken);
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        RefreshToken userRefreshToken = refreshTokenRepository.findByEmailAndDeviceName(username, deviceName)
                .orElseThrow(() -> new NoRefreshTokenException("Refresh token not found"));

        if (userRefreshToken.isRevoked() || userRefreshToken.getExpiryDate().before(new Date())) {
            throw new NoRefreshTokenException("Refresh token is expired or revoked");
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(username, "", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        return jwtProviderStrategyContext.get().generateAccessToken(authentication);
    }

    @Transactional
    public void saveRefreshToken(
            String refreshToken, String email,
            String deviceIpAddress, String deviceName, String deviceOs,
            String browser, String browserVersion
    ) {
        Date expiryDate = jwtProviderStrategyContext.get().getExpirationDateFromToken(refreshToken);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmailAndDeviceName(email, deviceName);

        RefreshToken token = existingToken.map(existing -> {
            existing.setToken(refreshToken);
            existing.setEmail(email);
            existing.setExpiryDate(expiryDate);
            existing.setDeviceName(deviceName);
            existing.setDeviceOs(deviceOs);
            existing.setBrowser(browser);
            existing.setBrowserVersion(browserVersion);
            existing.setRevoked(false);
            return existing;
        }).orElseGet(() -> RefreshToken.builder()
                        .token(refreshToken)
                        .email(email)
                        .expiryDate(expiryDate)
                        .deviceIpAddress(deviceIpAddress)
                        .deviceName(deviceName)
                        .deviceOs(deviceOs)
                        .browser(browser)
                        .browserVersion(browserVersion)
                        .build()
        );

        refreshTokenRepository.save(token);
    }

    public List<String> getAllDeviceNameByEmail(String email) {
        return refreshTokenRepository.findAllByEmail(email).stream()
               .map(RefreshToken::getDeviceName)
               .toList();
    }

    public List<RefreshToken> findAllValidByEmail(String email) {
        return refreshTokenRepository.findAllValidByEmail(email);
    }
}
