package com.example.bbmovie.security.oauth2;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.entity.enumerate.AuthProvider;
import com.example.bbmovie.entity.jwt.RefreshToken;
import com.example.bbmovie.repository.RefreshTokenRepository;
import com.example.bbmovie.security.JwtTokenProvider;
import com.example.bbmovie.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final Map<String, ProviderConfig> PROVIDER_CONFIGS = new HashMap<>();

    static {
        PROVIDER_CONFIGS.put("github", new ProviderConfig(
                "email", "name", "id", AuthProvider.GITHUB
        ));
        PROVIDER_CONFIGS.put("google", new ProviderConfig(
                "email", "name", "sub", AuthProvider.GOOGLE
        ));
        PROVIDER_CONFIGS.put("facebook", new ProviderConfig(
                "email", "name", "id", AuthProvider.FACEBOOK
        ));
    }

    private static class ProviderConfig {
        String emailAttribute;
        String nameAttribute;
        String userNameAttribute;
        AuthProvider authProvider;

        ProviderConfig(String emailAttribute, String nameAttribute, String userNameAttribute, AuthProvider authProvider) {
            this.emailAttribute = emailAttribute;
            this.nameAttribute = nameAttribute;
            this.userNameAttribute = userNameAttribute;
            this.authProvider = authProvider;
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
        ProviderConfig config = PROVIDER_CONFIGS.getOrDefault(provider, PROVIDER_CONFIGS.get("google"));

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        String email = attributes.getOrDefault(config.emailAttribute, "").toString();
        String name = attributes.getOrDefault(config.nameAttribute, "").toString();

        userService.findByEmail(email).ifPresentOrElse(user -> {
            updateAuthentication(authentication, attributes, config, user);
        }, () -> {
            User user = new User();
            user.setEmail(email);
            user.setRoles(Set.of("ROLE_USER"));
            String[] nameParts = name.split(" ");
            user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
            user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
            user.setAuthProvider(config.authProvider);
            userService.createUserFromOAuth2(user);
            updateAuthentication(authentication, attributes, config, user);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(accessToken);
        RefreshToken refreshTokenToDb = RefreshToken.builder()
                .token(refreshToken)
                .email(email)
                .expiryDate(expirationDate)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenToDb);
        response.addHeader("Set-Cookie",
                ResponseCookie.from("access-token", accessToken)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build()
                        .toString()
        );

        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(900);
        response.addCookie(cookie);

        String redirectUrl = frontendUrl + "/login?status=success?message=oauth2";
        this.setAlwaysUseDefaultTargetUrl(true);
        this.setDefaultTargetUrl(redirectUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void updateAuthentication(Authentication authentication, Map<String, Object> attributes, ProviderConfig config, User user) {
        DefaultOAuth2User newUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(
                        user.getRoles().toString().split(",")[0].trim())),
                attributes, config.userNameAttribute
        );
        Authentication securityAuth = new OAuth2AuthenticationToken(
                newUser,
                List.of(new SimpleGrantedAuthority(
                        user.getRoles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList().toString())
                ),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }
}