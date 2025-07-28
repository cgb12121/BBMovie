package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.NoRefreshTokenException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.RefreshTokenRepository;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.security.jose.config.JoseConstraint;
import com.bbmovie.auth.service.UserService;
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

    //TODO: need quick fixes asap, avoid inconsistence after update abac by getting info from db
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

        User user = userService.findByEmail(username).orElseThrow(() -> new UserNotFoundException("Unable to find user"));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(username, "", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        log.info("Refreshing access token for user {}", username);
        String sameSidWithRefreshToken = String.valueOf(userRefreshToken.getSid());
        //TODO: improve by overloading this method by accept claims/payload to prevent fetching from db
        //this approach allow to fetch new updated information immediately else will need to check blacklist token in cache
        return joseProviderStrategyContext.getActiveProvider().generateAccessToken(authentication, sameSidWithRefreshToken, user);
    }

    /**
     * saveRefreshToken assumes an existing RefreshToken for the sid, which may fail if the sid is new or deleted
     * => Token updates may fail silently, leading to inconsistent session state
     *
     * @param sid
     * @param refreshTokenString
     */
    @Transactional
    public void saveRefreshToken(String sid, String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findBySid(sid);
        if (refreshToken == null) {
            ;
        }
        Map<String, Object> claims = joseProviderStrategyContext.getActiveProvider().getClaimsFromToken(refreshTokenString);
        Date newExp = (Date) claims.get(JoseConstraint.JosePayload.EXP);
        Date newIat = (Date) claims.get(JoseConstraint.JosePayload.IAT);
        String jti = claims.get(JoseConstraint.JosePayload.JTI).toString();
        refreshToken.setExpiryDate(newExp);
        refreshToken.setCreatedDate(LocalDateTime.ofInstant(newIat.toInstant(), ZoneId.systemDefault()));
        refreshToken.setSid(sid);
        refreshToken.setJti(jti);
        refreshToken.setToken(refreshTokenString);
        refreshTokenRepository.save(refreshToken);
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
