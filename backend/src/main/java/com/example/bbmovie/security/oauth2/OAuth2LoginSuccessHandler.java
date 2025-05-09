package com.example.bbmovie.security.oauth2;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.entity.enumerate.AuthProvider;
import com.example.bbmovie.entity.enumerate.Role;
import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.security.jwt.asymmetric.JwtTokenPairedKeyProvider;
import com.example.bbmovie.service.UserService;
import com.example.bbmovie.service.auth.RefreshTokenService;
import com.example.bbmovie.utils.IpAddressUtils;
import com.example.bbmovie.utils.UserAgentAnalyzerUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nl.basjes.parse.useragent.UserAgent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenPairedKeyProvider jwtTokenPairedKeyProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserAgentAnalyzerUtils userAgentAnalyzer;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoderProvider.getIfAvailable();
    }

    private static final Map<String, ProviderConfig> PROVIDER_CONFIGS = initializeProviderConfigs();

    private static Map<String, ProviderConfig> initializeProviderConfigs() {
        Map<String, ProviderConfig> configs = new HashMap<>();
        configs.put("github", new ProviderConfig("login", "name", "id", "avatar_url", AuthProvider.GITHUB));
        configs.put("google", new ProviderConfig("email", "name", "sub", "avatar_url", AuthProvider.GOOGLE));
        configs.put("facebook", new ProviderConfig("email", "name", "id", "avatar_url", AuthProvider.FACEBOOK));
        configs.put("discord", new ProviderConfig("email", "global_name", "username", "avatar", AuthProvider.DISCORD));
        return configs;
    }

    private static class ProviderConfig {
        String emailAttribute;
        String nameAttribute;
        String userNameAttribute;
        String avatarUrlAttribute;
        AuthProvider authProvider;

        ProviderConfig(
                String emailAttribute,
                String nameAttribute,
                String userNameAttribute,
                String avatarUrlAttribute,
                AuthProvider authProvider
        ) {
            this.emailAttribute = emailAttribute;
            this.nameAttribute = nameAttribute;
            this.userNameAttribute = userNameAttribute;
            this.avatarUrlAttribute = avatarUrlAttribute;
            this.authProvider = authProvider;
        }
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        ProviderConfig config = PROVIDER_CONFIGS.get(provider);

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String name = Optional.ofNullable(attributes.get(config.nameAttribute))
                .map(Object::toString)
                .orElse(generateRandomUsername());
        String email = Optional.ofNullable(attributes.get(config.emailAttribute))
                .map(Object::toString)
                .orElse(generateRandomEmail());
        String avatarUrl = Optional.ofNullable(attributes.get(config.avatarUrlAttribute))
                .map(Object::toString)
                .orElse(generateRandomAvatarUrl());

        User user = userService.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setPassword(getPasswordEncoder().encode(generateRandomPasswordFoForOauth2()));
            newUser.setProfilePictureUrl(avatarUrl);
            newUser.setRole(Role.USER);
            String[] nameParts = name.split(" ");
            newUser.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
            StringBuilder lastNameBuilder = new StringBuilder();
            for (int i = 1; i < nameParts.length; i++) {
                lastNameBuilder.append(nameParts[i]);
                if (i < nameParts.length - 1) {
                    lastNameBuilder.append(" ");
                }
            }
            newUser.setLastName(lastNameBuilder.toString());
            newUser.setAuthProvider(config.authProvider);
            newUser.setIsEnabled(true);
            return userService.createUserFromOAuth2(newUser);
        });

        updateAuthentication(authentication, attributes, config, user);

        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);        String deviceName = agent.getValue("DeviceName");
        String deviceIpAddress = IpAddressUtils.getClientIp(request);
        String deviceOsName = agent.getValue("OperatingSystemName");
        String browser = agent.getValue("AgentName");
        String browserVersion = agent.getValue("AgentVersion");
        String accessToken = generateAccessTokenAndSaveRefreshTokenForOAuth2(
                authentication, email, deviceIpAddress, deviceName, deviceOsName, browser, browserVersion
        );

        response.addHeader("Set-Cookie",
                ResponseCookie.from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false) //set to true when using https
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build()
                        .toString()
        );

        String redirectUrl = frontendUrl + "/login?status=success&message=oauth2";
        setAlwaysUseDefaultTargetUrl(true);
        setDefaultTargetUrl(redirectUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String generateAccessTokenAndSaveRefreshTokenForOAuth2(
            Authentication authentication, String email, String deviceIpAddress,
            String deviceName, String deviceOs, String browser, String browserVersion
    ) {
        String accessToken = jwtTokenPairedKeyProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenPairedKeyProvider.generateRefreshToken(authentication);
        Date expirationDate = jwtTokenPairedKeyProvider.getExpirationDateFromToken(refreshToken);
        
        RefreshToken refreshTokenToDb = RefreshToken.builder()
                .token(refreshToken)
                .email(authentication.getName())
                .deviceIpAddress(deviceIpAddress)
                .expiryDate(expirationDate)
                .revoked(false)
                .build();
                
        refreshTokenService.saveRefreshToken(
                refreshTokenToDb.getToken(), email,
                deviceIpAddress, deviceName, deviceOs,
                browser, browserVersion
        );
        return accessToken;
    }

    private void updateAuthentication(Authentication authentication, Map<String, Object> attributes, ProviderConfig config, User user) {
        DefaultOAuth2User newUser = new DefaultOAuth2User(
                user.getAuthorities(), attributes, config.userNameAttribute
        );

        Authentication securityAuth = new OAuth2AuthenticationToken(
                newUser, user.getAuthorities(),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }

    private static String generateRandomPasswordFoForOauth2() {
        Random random = new SecureRandom();
        return RandomStringUtils.random(20, 0, 0, true,true, null, random);
    }

    private static String generateRandomUsername() {
        Random random = new SecureRandom();
        return RandomStringUtils.random(10, 0, 0, true,true, null, random);
    }

    private static String generateRandomEmail() {
        Random random = new SecureRandom();
        return RandomStringUtils.random(10, 0, 0, true,true, null, random) + "@bbmovie.com";
    }

    private static String generateRandomAvatarUrl() {
        int from1to1084cuzFreeApiHas1084img = new Random().nextInt(1084) + 1;
        return "https://picsum.photos/images#" + from1to1084cuzFreeApiHas1084img + "/200/200";
    }
}