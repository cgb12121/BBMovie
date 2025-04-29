package com.example.bbmovie.service.auth;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.ChangePasswordRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.request.ResetPasswordRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.exception.*;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.security.JwtTokenProvider;
import com.example.bbmovie.service.auth.verify.otp.OtpService;
import com.example.bbmovie.service.auth.verify.token.ChangePasswordTokenService;
import com.example.bbmovie.service.auth.verify.token.EmailVerifyTokenService;
import com.example.bbmovie.service.email.EmailService;
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
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerifyTokenService emailVerifyTokenService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final ChangePasswordTokenService changePasswordTokenService;
    private final OtpService otpService;

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
    public void verifyAccountByEmail(String token) {
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

    @Transactional
    @Override
    public void verifyAccountByOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }
        String email = otpService.getEmailForOtpToken(otp);
        if (email == null) {
            log.warn("Otp {} was already used or is invalid", otp);
            return;
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        if (user.isEnabled()) {
            log.info("Account {} already verified", email);
            return;
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        otpService.deleteOtpToken(otp);
    }

    @Override
    public void sendOtp(String email) {
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
    public void logout(String accessToken) {
        String email = tokenProvider.getUsernameFromToken(accessToken);
        refreshTokenService.deleteByEmail(email);
        tokenProvider.invalidateToken(accessToken);
    }

    @Override
    public UserResponse loadAuthenticatedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = CustomEmailException.class)
    public void changePassword(String requestEmail, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(requestEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found for username: " + requestEmail));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestEmail, request.getCurrentPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        boolean correctPassword = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
        if (!correctPassword) {
            throw new AuthenticationException("Current password is incorrect");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new AuthenticationException("New password can not be the same as the current password. Please try again.");
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AuthenticationException("New password and confirm password do not match. Please try again.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailService.notifyChangedPassword(user.getEmail());
    }

    @Override
    public void sendForgotPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        String token = changePasswordTokenService.generateChangePasswordToken(user);
        emailService.sendForgotPasswordEmail(email, token);
    }

    @Override
    public void resetPassword(String token, ResetPasswordRequest request) {
        String email = changePasswordTokenService.getEmailForToken(token);
        if (email == null) {
            log.warn("Reset password token {} was already used or is invalid", token);
            return;
        }
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found for email: " + email)
        );

        if (request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AuthenticationException("New password and confirm password do not match. Please try again.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        changePasswordTokenService.deleteToken(token);
        emailService.notifyChangedPassword(user.getEmail());
        log.info("Password reset for user {} successful", email);
    }
}