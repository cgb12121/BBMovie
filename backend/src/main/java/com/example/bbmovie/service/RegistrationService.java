package com.example.bbmovie.service;

import com.example.bbmovie.exception.*;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final TokenService tokenService;

    private static final String VERIFICATION_EMAIL_SUBJECT = "Verify your BBMovie account";
    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final int MAX_REGISTRATION_ATTEMPTS = 3;
    private static final long REGISTRATION_COOLDOWN_MINUTES = 15;
    private static final int MAX_VERIFICATION_EMAILS = 3;
    private static final long VERIFICATION_EMAIL_COOLDOWN_MINUTES = 5;

    // Rate limiting maps
    private final ConcurrentHashMap<String, RegistrationAttempt> registrationAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VerificationEmailAttempt> verificationEmailAttempts = new ConcurrentHashMap<>();

    @Transactional
    public User registerUser(User user) {
        // Check rate limiting
        checkRegistrationRateLimit(user.getEmail());

        // Validate password
        validatePassword(user.getPassword());

        // Check if username or email already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default role
        user.setRoles(Collections.singleton("ROLE_USER"));
        
        // Set account as not enabled
        user.setEnabled(false);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Generate verification token and send email
        String token = tokenService.generateVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        
        return savedUser;
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = tokenService.getEmailForToken(token);
        if (email == null) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(true);
        userRepository.save(user);
        
        // Delete the used token
        tokenService.deleteToken(token);
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.isEnabled()) {
            throw new RuntimeException("Email already verified");
        }
        
        // Generate new token and send email
        String token = tokenService.generateVerificationToken(user);
        emailService.sendVerificationEmail(email, token);
    }

    private void sendVerificationEmail(User user) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set email properties
        helper.setTo(user.getEmail());
        helper.setSubject(VERIFICATION_EMAIL_SUBJECT);

        // Create context for template
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("verificationUrl", "http://localhost:3000/verify-email?token=" + user.getEmailVerificationToken());

        // Process template
        String htmlContent = templateEngine.process("email/verification", context);
        helper.setText(htmlContent, true);

        // Send email
        mailSender.send(message);
    }

    private void checkRegistrationRateLimit(String email) {
        RegistrationAttempt attempt = registrationAttempts.computeIfAbsent(email, 
            k -> new RegistrationAttempt(MAX_REGISTRATION_ATTEMPTS, REGISTRATION_COOLDOWN_MINUTES));

        if (!attempt.canAttempt()) {
            throw new RateLimitExceededException(
                String.format("Too many registration attempts. Please try again in %d minutes", 
                attempt.getRemainingCooldownMinutes()));
        }

        attempt.recordAttempt();
    }

    private void checkVerificationEmailRateLimit(String email) {
        VerificationEmailAttempt attempt = verificationEmailAttempts.computeIfAbsent(email,
            k -> new VerificationEmailAttempt(MAX_VERIFICATION_EMAILS, VERIFICATION_EMAIL_COOLDOWN_MINUTES));

        if (!attempt.canAttempt()) {
            throw new RateLimitExceededException(
                String.format("Too many verification email requests. Please try again in %d minutes",
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
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character");
        }
    }

    // Rate limiting helper classes
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

    private static class VerificationEmailAttempt {
        private final int maxAttempts;
        private final long cooldownMinutes;
        private int attempts;
        private LocalDateTime lastAttempt;

        public VerificationEmailAttempt(int maxAttempts, long cooldownMinutes) {
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