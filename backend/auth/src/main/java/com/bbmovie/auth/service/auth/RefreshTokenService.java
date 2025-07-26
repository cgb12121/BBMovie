package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.NoRefreshTokenException;
import com.bbmovie.auth.repository.RefreshTokenRepository;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
@Log4j2
public class RefreshTokenService {

    private final JoseProviderStrategyContext joseProviderStrategyContext;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public RefreshTokenService(
            JoseProviderStrategyContext joseProviderStrategyContext,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.joseProviderStrategyContext = joseProviderStrategyContext;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void deleteByEmailAndDeviceName(String email, String deviceName) {
        refreshTokenRepository.deleteByEmailAndDeviceName(email, deviceName);
    }

    @Transactional
    public void deleteAllRefreshTokenByEmail(String email) {
        refreshTokenRepository.deleteAllByEmail(email);
    }

    public String refreshAccessToken(String oldAccessToken, String deviceName) {
        String username = joseProviderStrategyContext.getActiveProvider().getUsernameFromToken(oldAccessToken);
        List<String> roles = joseProviderStrategyContext.getActiveProvider().getRolesFromToken(oldAccessToken);
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
        log.info("Refreshing access token for user {}", username);
        String sameSidWithRefreshToken = String.valueOf(userRefreshToken.getSid());
        return joseProviderStrategyContext.getActiveProvider().generateAccessToken(authentication, sameSidWithRefreshToken);
    }

    @Transactional
    public void saveRefreshToken(
            String refreshToken, String email,
            String deviceIpAddress, String deviceName, String deviceOs,
            String browser, String browserVersion
    ) {
        Date expiryDate = joseProviderStrategyContext.getActiveProvider().getExpirationDateFromToken(refreshToken);
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
            log.info("Updated refresh token for user {}", email);
            return existing;
        }).orElseGet(() -> {
            log.info("Created new refresh token for user {}", email);
            return RefreshToken.builder()
                    .token(refreshToken)
                    .email(email)
                    .expiryDate(expiryDate)
                    .deviceIpAddress(deviceIpAddress)
                    .deviceName(deviceName)
                    .deviceOs(deviceOs)
                    .browser(browser)
                    .browserVersion(browserVersion)
                    .revoked(false)
                    .build();
                }
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
