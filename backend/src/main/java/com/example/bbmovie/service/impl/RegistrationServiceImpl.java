package com.example.bbmovie.service.impl;

import com.example.bbmovie.exception.*;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.service.intf.EmailService;
import com.example.bbmovie.service.intf.RegistrationService;
import com.example.bbmovie.dto.request.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailVerifyTokenService tokenService;

    private static final int MAX_REGISTRATION_ATTEMPTS = 3;
    private static final long REGISTRATION_COOLDOWN_MINUTES = 15;

    private final ConcurrentHashMap<String, RegistrationAttempt> registrationAttempts = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(Collections.singleton("ROLE_USER"));
        user.setIsEnabled(false);

        User savedUser = userRepository.save(user);

        String verificationToken = tokenService.generateVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        return savedUser;
    }

    @Transactional
    @Override
    public void verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }

        String email = tokenService.getEmailForToken(token);
        if (email == null) {
            throw new TokenVerificationException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        
        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        
        user.setIsEnabled(true);
        userRepository.save(user);
        
        tokenService.deleteToken(token);
    }


    @Override
    public void sendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        
        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        
        try {
            String token = tokenService.generateVerificationToken(user);
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage() + "," + e);
        }
    }


    private void checkRegistrationRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }

        RegistrationAttempt attempt = registrationAttempts.computeIfAbsent(ipAddress, 
            k -> new RegistrationAttempt(MAX_REGISTRATION_ATTEMPTS, REGISTRATION_COOLDOWN_MINUTES));

        if (!attempt.canAttempt()) {
            throw new RateLimitExceededException(
                String.format("Too many registration attempts. Please try again in %d minutes", 
                attempt.getRemainingCooldownMinutes()));
        }

        attempt.recordAttempt();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character");
        }
    }

    private String getClientIP() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String ipAddress = request.getHeader("X-Forwarded-For");
        String unknownHost = "unknown";
        if (ipAddress == null || ipAddress.isEmpty() || unknownHost.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || unknownHost.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || unknownHost.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || unknownHost.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || unknownHost.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    private static class RegistrationAttempt {
        private final int maxAttempts;
        private final long cooldownMinutes;
        private int attempts;
        private LocalDateTime lastAttempt;

        public RegistrationAttempt(int maxAttempts, long cooldownMinutes) {
            this.maxAttempts = maxAttempts;
            this.cooldownMinutes = cooldownMinutes;
            this.attempts = 0;
            this.lastAttempt = LocalDateTime.now();
        }

        public boolean canAttempt() {
            if (attempts >= maxAttempts) {
                long minutesSinceLastAttempt = TimeUnit.MINUTES.convert(
                    java.time.Duration.between(lastAttempt, LocalDateTime.now()).toNanos(),
                    TimeUnit.NANOSECONDS);
                return minutesSinceLastAttempt >= cooldownMinutes;
            }
            return true;
        }

        public void recordAttempt() {
            attempts++;
            lastAttempt = LocalDateTime.now();
        }

        public long getRemainingCooldownMinutes() {
            long minutesSinceLastAttempt = TimeUnit.MINUTES.convert(
                java.time.Duration.between(lastAttempt, LocalDateTime.now()).toNanos(),
                TimeUnit.NANOSECONDS);
            return Math.max(0, cooldownMinutes - minutesSinceLastAttempt);
        }
    }
}