package com.example.bbmovie.security.oauth2;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.entity.enumerate.Role;
import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.security.jwt.asymmetric.JwtTokenPairedKeyProvider;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategyFactory;
import com.example.bbmovie.service.UserService;
import com.example.bbmovie.service.auth.RefreshTokenService;
import com.example.bbmovie.utils.CreateUserUtils;
import com.example.bbmovie.utils.IpAddressUtils;
import com.example.bbmovie.utils.UserAgentAnalyzerUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nl.basjes.parse.useragent.UserAgent;
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
import java.time.Duration;
import java.util.*;

@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserService userService;
    private final JwtTokenPairedKeyProvider jwtTokenPairedKeyProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserAgentAnalyzerUtils userAgentAnalyzer;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final OAuth2UserInfoStrategyFactory strategyFactory;

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
        log.info("OAuth2 login success for provider: {}", oAuth2Token);
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        OAuth2UserInfoStrategy strategy = strategyFactory.getStrategy(provider);
        log.info("OAuth2 login success for provider: {} with strategy: {}", provider, strategy);

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        String name = Optional.ofNullable(strategy.getName(attributes))
                .orElse(CreateUserUtils.generateRandomUsername());
        String email = Optional.ofNullable(strategy.getEmail(attributes))
                .orElse(CreateUserUtils.generateRandomEmail());
        String avatarUrl = Optional.ofNullable(strategy.getAvatarUrl(attributes))
                .orElse(CreateUserUtils.generateRandomAvatarUrl());

        User user = userService.findByEmail(email).orElseGet(() -> {
            String randomPasswordForOauth2 = CreateUserUtils.generateRandomPasswordFoForOauth2();
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
            User newUser = User.builder()
                    .email(email)
                    .username(email)
                    .password(encodedPassword)
                    .profilePictureUrl(avatarUrl)
                    .firstName(firstName)
                    .lastName(lastNameBuilder.toString())
                    .role(Role.USER)
                    .isAccountNonExpired(true)
                    .authProvider(strategy.getAuthProvider())
                    .build();
            return userService.createUserFromOAuth2(newUser);
        });

        updateAuthentication(authentication, attributes, strategy, user);

        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);
        String deviceName = agent.getValue("DeviceName");
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

    private void updateAuthentication(
            Authentication authentication,
            Map<String, Object> attributes,
            OAuth2UserInfoStrategy strategy,
            User user
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
}