package com.example.bbmovie.service.impl;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.AccessTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.exception.*;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.security.JwtTokenProvider;
import com.example.bbmovie.service.EmailVerifyTokenService;
import com.example.bbmovie.service.intf.AuthService;
import com.example.bbmovie.service.intf.EmailService;
import com.example.bbmovie.service.intf.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerifyTokenService emailVerifyTokenService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = registerUser(request);
        return AuthResponse.builder()
                .email(user.getEmail())
                .build();
    }

    private User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(Collections.singleton("ROLE_USER"));
        user.setIsEnabled(false);

        User savedUser = userRepository.save(user);

        String verificationToken = emailVerifyTokenService.generateVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        return savedUser;
    }

    @Transactional
    @Override
    public void verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }
        String email = emailVerifyTokenService.getEmailForToken(token);
        if (email == null) {
            log.warn("Token {} was already used or is invalid", token);
            return;
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        if (user.isEnabled()) {
            log.info("Email {} already verified", email);
            return;
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        emailVerifyTokenService.deleteToken(token);
    }

    @Override
    public void sendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));

        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        try {
            String token = emailVerifyTokenService.generateVerificationToken(user);
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email or password"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean isUserEnabled = user.isEnabled();
        if (!isUserEnabled) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }

        boolean correctPassword = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!correctPassword) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

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