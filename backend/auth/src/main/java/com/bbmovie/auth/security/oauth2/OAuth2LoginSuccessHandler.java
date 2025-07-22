package com.bbmovie.auth.security.oauth2;

import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategyFactory;
import com.bbmovie.auth.service.UserService;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import com.bbmovie.auth.utils.DeviceInfoUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserService userService;
    private final JoseProviderStrategyContext joseProviderStrategyContext;
    private final RefreshTokenService refreshTokenService;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final OAuth2UserInfoStrategyFactory strategyFactory;
    private final DeviceInfoUtils deviceInfoUtils;

    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoderProvider.getIfAvailable();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        OAuth2UserInfoStrategy strategy = strategyFactory.getStrategy(provider);

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        User user = createAndSavaUserFromOAuth2(strategy, attributes);
        updateAuthentication(authentication, attributes, strategy, user);

        UserAgentResponse userAgentInfo = deviceInfoUtils.extractUserAgentInfo(request);

        String accessToken = generateAccessTokenAndSaveRefreshTokenForOAuth2(
                authentication, user.getEmail(), userAgentInfo
        );

        addAccessTokenToCookie(response, accessToken);

        String redirectUrl = frontendUrl + "/login?status=success&message=" +
                URLEncoder.encode("login via oauth2 success", StandardCharsets.UTF_8);
        setAlwaysUseDefaultTargetUrl(true);
        setDefaultTargetUrl(redirectUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private User createAndSavaUserFromOAuth2(
            OAuth2UserInfoStrategy strategy,
            Map<String, Object> attributes
    ) {
        String name = Optional.ofNullable(strategy.getName(attributes))
                .orElse(generateRandomUsername());
        String email = Optional.ofNullable(strategy.getEmail(attributes))
                .orElse(generateRandomEmail());
        String avatarUrl = Optional.ofNullable(strategy.getAvatarUrl(attributes))
                .orElse(generateRandomAvatarUrl());

        return userService.findByEmail(email).orElseGet(() -> {
            String randomPasswordForOauth2 = generateRandomPasswordFoForOauth2();
            String encodedPassword = getPasswordEncoder().encode(randomPasswordForOauth2);
            String[] nameParts = name.split(" ");
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            StringBuilder lastNameBuilder = new StringBuilder();
            for (int i = 1; i < nameParts.length; i++) {
                lastNameBuilder.append(nameParts[i]);
                if (i < nameParts.length - 1) {
                    lastNameBuilder.append(" ");
                }
            }
            String lastName = lastNameBuilder.toString();

            User newUser = createUserForOauth2(
                    email, encodedPassword, avatarUrl, firstName, lastName, strategy
            );
            return userService.createUserFromOAuth2(newUser);
        });
    }

    private String generateAccessTokenAndSaveRefreshTokenForOAuth2(
            Authentication authentication, String email, UserAgentResponse userAgentInfo
    ) {
        String sid = UUID.randomUUID().toString();
        String accessToken = joseProviderStrategyContext.getActiveProvider().generateAccessToken(authentication, sid);
        String refreshToken = joseProviderStrategyContext.getActiveProvider().generateRefreshToken(authentication, sid);
        Date expirationDate = joseProviderStrategyContext.getActiveProvider().getExpirationDateFromToken(refreshToken);

        RefreshToken refreshTokenToDb = RefreshToken.builder()
                .token(refreshToken)
                .email(authentication.getName())
                .deviceIpAddress(userAgentInfo.getDeviceIpAddress())
                .expiryDate(expirationDate)
                .revoked(false)
                .build();

        refreshTokenService.saveRefreshToken(
                refreshTokenToDb.getToken(), email,
                userAgentInfo.getDeviceIpAddress(), userAgentInfo.getDeviceName(), userAgentInfo.getDeviceOs(),
                userAgentInfo.getBrowser(), userAgentInfo.getBrowserVersion()
        );
        return accessToken;
    }

    private void addAccessTokenToCookie(HttpServletResponse response, String accessToken) {
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
    }

    private void updateAuthentication(
            Authentication authentication, Map<String, Object> attributes,
            OAuth2UserInfoStrategy strategy, User user
    ) {
        String usernameAttributeKey  = strategy.getEmailAttributeKey(attributes);
        DefaultOAuth2User newUser = new DefaultOAuth2User(
                user.getAuthorities(), attributes, usernameAttributeKey
        );

        Authentication securityAuth = new OAuth2AuthenticationToken(
                newUser, user.getAuthorities(),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }

    private static User createUserForOauth2(
            String email, String encodedPassword, String avatarUrl, String firstName, String lastName,
            OAuth2UserInfoStrategy strategy
    ) {
        return User.builder()
                .email(email)
                .displayedUsername(email)
                .password(encodedPassword)
                .profilePictureUrl(avatarUrl)
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .lastLoginTime(LocalDateTime.now())
                .authProvider(strategy.getAuthProvider())
                .build();
    }

    private static final Random random = new Random();

    private static String generateRandomPasswordFoForOauth2() {
        return RandomStringUtils.random(20, 0, 0, true,true, null, random);
    }

    private static String generateRandomUsername() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random);
    }

    private static String generateRandomEmail() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random) + "@bbmovie.com";
    }

    private static String generateRandomAvatarUrl() {
        int from1to1084cuzFreeApiHas1084img = random.nextInt(1084) + 1;
        return "https://picsum.photos/id/" + from1to1084cuzFreeApiHas1084img + "/200/200";
    }
}