package com.example.bbmovie.service.impl;

import com.example.bbmovie.exception.*;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.service.intf.LoginService;
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

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_COOLDOWN_MINUTES = 15;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long ACCOUNT_LOCKOUT_MINUTES = 30;

    private final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailedLoginAttempt> failedLoginAttempts = new ConcurrentHashMap<>();

    @Transactional
    @Override
    public User authenticateUser(String usernameOrEmail, String password) {
        checkLoginRateLimit(getClientIP());

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email or password"));

        checkAccountLockout(user.getUsername());

        if (!user.isEnabled()) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailedLogin(user.getUsername());
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        resetFailedLoginAttempts(user.getUsername());

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    private void checkLoginRateLimit(String ipAddress) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(ipAddress,
            k -> new LoginAttempt(MAX_LOGIN_ATTEMPTS, LOGIN_COOLDOWN_MINUTES));

        if (!attempt.canAttempt()) {
            throw new RateLimitExceededException(
                String.format("Too many login attempts. Please try again in %d minutes",
                attempt.getRemainingCooldownMinutes()));
        }

        attempt.recordAttempt();
    }

    private void checkAccountLockout(String username) {
        FailedLoginAttempt attempt = failedLoginAttempts.get(username);
        if (attempt != null && attempt.isLocked()) {
            throw new AccountLockedException(
                String.format("Account is locked. Please try again in %d minutes",
                attempt.getRemainingLockoutMinutes()));
        }
    }

    private void recordFailedLogin(String username) {
        FailedLoginAttempt attempt = failedLoginAttempts.computeIfAbsent(username,
            k -> new FailedLoginAttempt(MAX_FAILED_ATTEMPTS, ACCOUNT_LOCKOUT_MINUTES));

        attempt.recordFailedAttempt();
        if (attempt.isLocked()) {
            throw new AccountLockedException(
                String.format("Account is locked due to too many failed attempts. Please try again in %d minutes",
                attempt.getRemainingLockoutMinutes())
            );
        }
    }

    private void resetFailedLoginAttempts(String username) {
        failedLoginAttempts.remove(username);
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

    private static class LoginAttempt {
        private final int maxAttempts;
        private final long cooldownMinutes;
        private int attempts;
        private LocalDateTime lastAttempt;

        public LoginAttempt(int maxAttempts, long cooldownMinutes) {
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

    private static class FailedLoginAttempt {
        private final int maxAttempts;
        private final long lockoutMinutes;
        private int attempts;
        private LocalDateTime lastAttempt;
        private LocalDateTime lockoutTime;

        public FailedLoginAttempt(int maxAttempts, long lockoutMinutes) {
            this.maxAttempts = maxAttempts;
            this.lockoutMinutes = lockoutMinutes;
            this.attempts = 0;
            this.lastAttempt = LocalDateTime.now();
        }

        public void recordFailedAttempt() {
            attempts++;
            lastAttempt = LocalDateTime.now();
            if (attempts >= maxAttempts) {
                lockoutTime = lastAttempt;
            }
        }

        public boolean isLocked() {
            if (lockoutTime == null) {
                return false;
            }
            long minutesSinceLockout = TimeUnit.MINUTES.convert(
                java.time.Duration.between(lockoutTime, LocalDateTime.now()).toNanos(),
                TimeUnit.NANOSECONDS);
            return minutesSinceLockout < lockoutMinutes;
        }

        public long getRemainingLockoutMinutes() {
            if (lockoutTime == null) {
                return 0;
            }
            long minutesSinceLockout = TimeUnit.MINUTES.convert(
                java.time.Duration.between(lockoutTime, LocalDateTime.now()).toNanos(),
                TimeUnit.NANOSECONDS);
            return Math.max(0, lockoutMinutes - minutesSinceLockout);
        }
    }
} 