package com.example.bbmovie.service.impl;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.AccessTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.exception.InvalidTokenException;
import com.example.bbmovie.model.User;
import com.example.bbmovie.security.JwtTokenProvider;
import com.example.bbmovie.service.intf.AuthService;
import com.example.bbmovie.service.intf.LoginService;
import com.example.bbmovie.service.intf.RefreshTokenService;
import com.example.bbmovie.service.intf.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final LoginService loginService;
    private final RegistrationService registrationService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        User user = registrationService.registerUser(request);
        return AuthResponse.builder()
                .email(user.getEmail())
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = loginService.login(request.getEmail(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRoles().toString())
                .build();
    }

    @Override
    public AuthResponse refreshToken(AccessTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!refreshTokenService.isValidRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = refreshTokenService.getUsernameFromRefreshToken(refreshToken);
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null);
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = refreshTokenService.createRefreshToken(email);
        refreshTokenService.deleteRefreshToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(email)
                .build();
    }

    @Override
    public void logout(AccessTokenRequest request) {
        refreshTokenService.deleteRefreshToken(request.getRefreshToken());
    }
} 