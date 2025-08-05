package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.NoRefreshTokenException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.RefreshTokenRepository;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.service.UserService;
import com.example.common.entity.JoseConstraint;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.common.entity.JoseConstraint.JosePayload.*;

@Service
@Log4j2
public class RefreshTokenService {

    private final JoseProviderStrategyContext joseProviderStrategyContext;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Autowired
    public RefreshTokenService(
            JoseProviderStrategyContext joseProviderStrategyContext,
            RefreshTokenRepository refreshTokenRepository,
            UserService userService
    ) {
        this.joseProviderStrategyContext = joseProviderStrategyContext;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    @Transactional
    public void deleteByEmailAndSid(String email, String sid) {
        refreshTokenRepository.deleteByEmailAndSid(email, sid);
    }

    @Transactional
    public void deleteAllRefreshTokenByEmail(String email) {
        refreshTokenRepository.deleteAllByEmail(email);
    }

    //TODO: "One-Time Use" Refresh Token: Should also create a new refresh token to handle replay attack (optional)
    //The current approach is "Multi-Use" Refresh Token
    public String refreshAccessToken(String oldAccessToken) {
        if (oldAccessToken == null || oldAccessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        if (oldAccessToken.startsWith("Bearer ")) {
            oldAccessToken = oldAccessToken.substring(7);
        }

        JoseProviderStrategy joseProviderStrategy = joseProviderStrategyContext.getActiveProvider();
        Map<String, Object> claims = joseProviderStrategy.getClaimsFromToken(oldAccessToken);
        String username = claims.get(JoseConstraint.JosePayload.SUB).toString();
        String sid = claims.get(JoseConstraint.JosePayload.SID).toString();
        String roleListString = claims.get(JoseConstraint.JosePayload.ROLE).toString();
        List<String> roles;
        if (roleListString != null) {
            roles = Arrays.asList(roleListString.split(","));
        } else {
            roles = List.of();
        }

        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        RefreshToken userRefreshToken = refreshTokenRepository.findByEmailAndSid(username, sid)
                .orElseThrow(() -> new NoRefreshTokenException("Refresh token not found"));

        if (userRefreshToken.isRevoked() || userRefreshToken.getExpiryDate().before(new Date())) {
            throw new NoRefreshTokenException("Refresh token is expired or revoked");
        }

        User user = userService.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("Unable to find user"));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, "", authorities
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, "", authorities
        );

        log.info("Refreshing access token for user {}", username);
        String sameSidWithRefreshToken = String.valueOf(userRefreshToken.getSid());
        return joseProviderStrategy.generateAccessToken(authentication, sameSidWithRefreshToken, user);
    }

    /**
     * saveRefreshToken assumes an existing RefreshToken for the sid, which may fail if the sid is new or deleted
     * => Token updates may fail silently, leading to an inconsistent session state
     */
    @Transactional
    public void saveRefreshToken(String sid, String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findBySid(sid);
        if (refreshToken != null) {
            Map<String, Object> claims = joseProviderStrategyContext.getActiveProvider().getClaimsFromToken(refreshTokenString);
            Date newExp = (Date) claims.get(EXP);
            Date newIat = (Date) claims.get(JoseConstraint.JosePayload.IAT);
            String jti = claims.get(JoseConstraint.JosePayload.JTI).toString();
            refreshToken.setExpiryDate(newExp);
            refreshToken.setCreatedDate(LocalDateTime.ofInstant(newIat.toInstant(), ZoneId.systemDefault()));
            refreshToken.setSid(sid);
            refreshToken.setJti(jti);
            refreshToken.setToken(refreshTokenString);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional
    public void saveRefreshToken(
            String refreshToken, String email,
            String deviceIpAddress, String deviceName, String deviceOs,
            String browser, String browserVersion
    ) {
        Map<String, Object> claims = joseProviderStrategyContext.getActiveProvider().getClaimsFromToken(refreshToken);
        Date expiryDate = (Date) claims.get(EXP);
        String jti = (String) claims.get(JTI);
        String sid = (String) claims.get(SID);
        Optional<RefreshToken> existingToken = Optional.ofNullable(refreshTokenRepository.findBySid(sid));

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
                    .jti(jti)
                    .sid(sid)
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

    public List<RefreshToken> findAllValidByEmail(String email) {
        return refreshTokenRepository.findAllValidByEmail(email);
    }

    public void deleteRefreshToken(String sid) {
        refreshTokenRepository.deleteBySid(sid);
    }

    public List<String> getAllSessionsByEmail(String email) {
        return refreshTokenRepository.findAllByEmail(email) // Get all refresh token logged in by email
                .stream()
                .map(RefreshToken::getSid)
                .toList();
    }
}
