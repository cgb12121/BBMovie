package com.example.bbmovie.security.oauth2;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.entity.enumerate.AuthProvider;
import com.example.bbmovie.entity.enumerate.Role;
import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.security.JwtTokenProvider;
import com.example.bbmovie.service.UserService;
import com.example.bbmovie.service.auth.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Handles successful OAuth2 login by processing user data, generating jwt tokens, and redirecting to the frontend.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoderProvider.getIfAvailable();
    }

    private static final Map<String, ProviderConfig> PROVIDER_CONFIGS = initializeProviderConfigs();

    private static Map<String, ProviderConfig> initializeProviderConfigs() {
        Map<String, ProviderConfig> configs = new HashMap<>();
        configs.put("github",
                new ProviderConfig(
                        "login",
                        "name",
                        "id",
                        "avatar_url",
                        AuthProvider.GITHUB)
        );
        configs.put("google",
                new ProviderConfig(
                        "email",
                        "name",
                        "sub",
                        "avatar_url",
                        AuthProvider.GOOGLE)
        );
        configs.put("facebook",
                new ProviderConfig(
                        "email",
                        "name",
                        "id",
                        "avatar_url",
                        AuthProvider.FACEBOOK)
        );
        return configs;
    }

    private static class ProviderConfig {
        String emailAttribute;
        String nameAttribute;
        String userNameAttribute;
        String avatarUrlAttribute;
        AuthProvider authProvider;

        ProviderConfig(String emailAttribute, String nameAttribute, String userNameAttribute,
                       String avatarUrlAttribute, AuthProvider authProvider) {
            this.emailAttribute = emailAttribute;
            this.nameAttribute = nameAttribute;
            this.userNameAttribute = userNameAttribute;
            this.avatarUrlAttribute = avatarUrlAttribute;
            this.authProvider = authProvider;
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        ProviderConfig config = PROVIDER_CONFIGS.get(provider);

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        String email = attributes.getOrDefault(config.emailAttribute, "email").toString();
        String name = attributes.getOrDefault(config.nameAttribute, "name").toString();
        String avatarUrl = attributes.getOrDefault(config.avatarUrlAttribute, "avatar_url").toString();

        User user = userService.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setPassword(getPasswordEncoder().encode(generateRandomPasswordFoForOauth2()));
            newUser.setProfilePictureUrl(avatarUrl);
            newUser.setRole(Role.USER);
            String[] nameParts = name.split(" ");
            newUser.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
            newUser.setLastName(nameParts.length > 1 ? nameParts[1] : "");
            newUser.setAuthProvider(config.authProvider);
            newUser.setIsEnabled(true);
            return userService.createUserFromOAuth2(newUser);
        });

        updateAuthentication(authentication, attributes, config, user);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(refreshToken);

        RefreshToken refreshTokenToDb = RefreshToken.builder()
                .token(refreshToken)
                .email(email)
                .expiryDate(expirationDate)
                .revoked(false)
                .build();

        refreshTokenService.saveRefreshToken(refreshTokenToDb.getToken(), user.getEmail());

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

    private void updateAuthentication(Authentication authentication, Map<String, Object> attributes,
                                      ProviderConfig config, User user) {
        DefaultOAuth2User newUser = new DefaultOAuth2User(
                user.getAuthorities(), attributes, config.userNameAttribute
        );

        Authentication securityAuth = new OAuth2AuthenticationToken(
                newUser, user.getAuthorities(),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }

    private String generateRandomPasswordFoForOauth2() {
        Random random = new SecureRandom();
        return RandomStringUtils.random(20, 0, 0, true, true, null, random);
    }
}