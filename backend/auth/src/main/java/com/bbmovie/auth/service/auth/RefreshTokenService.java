package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.BlacklistedJwtTokenException;
import com.bbmovie.auth.exception.InvalidTokenException;
import com.bbmovie.auth.exception.NoRefreshTokenException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.RefreshTokenRepository;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.service.auth.user.UserService;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
@SuppressWarnings("ConstantConditions") // Suppress warning on passing Nonnull (on purpose)
public class RefreshTokenService {

    private final JoseProvider joseProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final RefreshTokenService selfProxy; // spring should solve this proxied self-reference instance

    @Autowired
    public RefreshTokenService(
            JoseProvider joseProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserService userService,
            @Lazy RefreshTokenService selfProxy
    ) {
        this.joseProvider = joseProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.selfProxy = selfProxy;
    }

    @Transactional
    public void deleteByEmailAndSid(String email, String sid) {
        refreshTokenRepository.deleteByEmailAndSid(email, sid);
    }

    @Transactional
    public void deleteAllRefreshTokenByEmail(String email) {
        refreshTokenRepository.deleteAllByEmail(email);
    }

    public String refreshAccessToken(String oldAccessToken) {
        if (oldAccessToken == null || oldAccessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        if (oldAccessToken.startsWith("Bearer ")) {
            oldAccessToken = oldAccessToken.substring(7);
        }

        if (!joseProvider.validateToken(oldAccessToken)) {
            throw new InvalidTokenException("Token is invalid, expired, or signed with an unknown key.");
        }

        Map<String, Object> claims = joseProvider.getClaimsFromToken(oldAccessToken);
        String username = (String) claims.get(SUB);
        String sid = (String) claims.get(SID);

        if (joseProvider.isTokenInLogoutBlacklist(sid)) {
            throw new BlacklistedJwtTokenException("Session has logged out or expired.");
        }

        RefreshToken userRefreshToken = refreshTokenRepository.findByEmailAndSid(username, sid)
                .orElseThrow(() -> new NoRefreshTokenException("Refresh token not found"));

        if (userRefreshToken.isRevoked() || userRefreshToken.getExpiryDate().before(new Date())) {
            throw new NoRefreshTokenException("Refresh token is expired or revoked");
        }

        User user = userService.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("Unable to find user"));

        List<SimpleGrantedAuthority> authorities = joseProvider.getRolesFromToken(oldAccessToken).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, "", authorities
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = joseProvider.generateAccessToken(authentication, sid, user);

        if (joseProvider.isTokenInABACBlacklist(sid)) {
            String newRefreshTokenString = joseProvider.generateRefreshToken(authentication, sid, user);
            selfProxy.saveRefreshToken(sid, newRefreshTokenString);
            joseProvider.removeTokenFromABACBlacklist(sid);
        }

        return newAccessToken;
    }

    @Transactional
    public void saveRefreshToken(String sid, String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findBySid(sid);
        if (refreshToken != null) {
            if (!joseProvider.validateToken(refreshTokenString)) {
                log.warn("Cannot save refresh token; it was signed with an unknown key.");
                return;
            }
            Map<String, Object> claims = joseProvider.getClaimsFromToken(refreshTokenString);
            Date newExp = (Date) claims.get(EXP);
            Date newIat = (Date) claims.get(IAT);
            String jti = claims.get(JTI).toString();
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
        if (!joseProvider.validateToken(refreshToken)) {
            log.error("Cannot save refresh token for user {}; it was signed with an unknown or invalid key.", email);
            throw new InvalidTokenException("Cannot process refresh token with invalid signature.");
        }

        Map<String, Object> claims = joseProvider.getClaimsFromToken(refreshToken);
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
        return refreshTokenRepository.findAllByEmail(email)
                .stream()
                .map(RefreshToken::getSid)
                .toList();
    }
}
