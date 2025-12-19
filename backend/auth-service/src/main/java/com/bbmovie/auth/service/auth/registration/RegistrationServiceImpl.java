package com.bbmovie.auth.service.auth.registration;

import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.exception.*;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.SecurityConfig;
import com.bbmovie.auth.service.auth.verify.magiclink.EmailVerifyTokenService;
import com.bbmovie.auth.service.auth.verify.otp.OtpService;
import com.bbmovie.auth.service.nats.EmailEventProducer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

import static com.bbmovie.auth.constant.AuthErrorMessages.EMAIL_ALREADY_EXISTS;
import static com.bbmovie.auth.constant.UserErrorMessages.USER_NOT_FOUND_BY_EMAIL;
import static com.bbmovie.auth.security.SecurityConfig.ACTIVE_ENCODER;

@Log4j2
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerifyTokenService emailVerifyTokenService;
    private final EmailEventProducer emailEventProducer;
    private final OtpService otpService;

    @Autowired
    public RegistrationServiceImpl(
            UserRepository userRepository, EmailVerifyTokenService emailVerifyTokenService,
            EmailEventProducer emailEventProducer, OtpService otpService) {
        this.userRepository = userRepository;
        this.emailVerifyTokenService = emailVerifyTokenService;
        this.emailEventProducer = emailEventProducer;
        this.otpService = otpService;
    }

    /**
     * Registers a new user account with the provided registration details.
     * Ensures email uniqueness, validates passwords, and generates a verification token.
     *
     * @param request the registration details containing the user's email, password,
     *                and confirmation password
     * @throws EmailAlreadyExistsException if the email address already exists in the system
     * @throws AuthenticationException if the password and confirmation password does not match
     */
    @Transactional
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        log.info("password: {}, {}", request.getPassword(), request.getConfirmPassword());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Password and confirm password does not match. Please try again.");
        }

        User user = createUserFromRegisterRequest(request);
        User savedUser = userRepository.save(user);

        String verificationToken = emailVerifyTokenService.generateVerificationToken(savedUser);
        emailEventProducer.sendMagicLinkOnRegistration(savedUser.getEmail(), verificationToken);
    }

    /**
     * Verifies the user account using the provided email verification token.
     * If the token is valid and the user's account is not yet enabled,
     * the user's account is activated, and the token is deleted.
     * Otherwise, appropriate exceptions are thrown for invalid tokens or other errors.
     *
     * @param token the email verification token used to verify the user account
     * @return a success message indicating that the account verification is completed,
     *         or that the account was already verified
     * @throws TokenVerificationException if the token is null or empty
     * @throws AuthenticationException if the token is invalid or the user cannot be verified
     */
    @Transactional
    @Override
    public String verifyAccountByEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }
        String email = emailVerifyTokenService.getEmailForToken(token);
        if (email == null) {
            log.warn("Token {} was already used or is invalid", token);
            throw new AuthenticationException("Account verification failed. Please try again.");
        }

        log.info("Trying to verify: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AuthenticationException("Unable to verify user"));
        if (user.isEnabled()) {
            log.info("Email {} already verified", email);
            return "Account already verified.";
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        emailVerifyTokenService.deleteToken(token);

        return "Account verification successful. Please login to continue.";
    }

    /**
     * Sends a verification email to a user with the specified email address. This method generates
     * a verification token and sends it to the user's email. If the email is already verified or
     * the user does not exist, an appropriate exception is thrown.
     *
     * @param email the email address to which the verification email will be sent
     *              (must not be null or empty)
     * @throws IllegalArgumentException if the email is null or empty
     * @throws UserNotFoundException if no user exists with the given email address
     * @throws EmailAlreadyVerifiedException if the email is already verified
     * @throws TokenVerificationException if an error occurs during the token generation or sending process
     */
    @Override
    public void sendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format(USER_NOT_FOUND_BY_EMAIL, email)
                ));

        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        try {
            String token = emailVerifyTokenService.generateVerificationToken(user);
            emailEventProducer.sendMagicLinkOnRegistration(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Verifies a user's account using the provided OTP (One-Time Password).
     * This method enables the user's account if the OTP is valid, not previously used,
     * and associated with a registered email. If the OTP is null, empty, or invalid,
     * an appropriate exception or log entry is generated.
     *
     * @param otp the One-Time Password provided by the user for account verification
     * @throws TokenVerificationException if the OTP is null or empty
     * @throws UserNotFoundException if no user is found for the email linked to the OTP
     */
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format(USER_NOT_FOUND_BY_EMAIL, email)
                ));
        if (user.isEnabled()) {
            log.info("Account {} already verified", email);
            return;
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        otpService.deleteOtpToken(otp);
    }

    /**
     * Sends a one-time password (OTP) to the user's phone number.
     * This method generates an OTP token for the specified user and sends it
     * using an email event producer.
     *
     * @param user the user for whom the OTP will be generated and sent. The user must have a valid phone number.
     * @throws IllegalArgumentException if the user's phone number is null or empty.
     */
    @Override
    public void sendOtp(User user) {
        if (user.getPhoneNumber().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String otp = otpService.generateOtpToken(user);
        emailEventProducer.sendOtp(user.getPhoneNumber(), otp);
    }

    /**
     * Creates a new User object based on the provided RegisterRequest.
     *
     * @param request the register request containing the user details such as email, username, password, phone number, first name, last name, age, and region
     * @return a newly created User instance with the provided details and default settings for roles, authentication provider, and account status
     */
    private User createUserFromRegisterRequest(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail())
                .displayedUsername(request.getUsername())
                .password(encodeWithRandomAlgorithm(request.getPassword()))
//                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
//                .age(request.getAge())
//                .region(request.getRegion())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }

    private String encodeWithRandomAlgorithm(String rawPassword) {
        List<String> algos = SecurityConfig.getActiveEncodersName();
        
        String chosenId = algos.get(new SecureRandom().nextInt(algos.size()));
        
        PasswordEncoder encoder = ACTIVE_ENCODER.get(chosenId);
        
        if (encoder == null) {
            throw new RuntimeException("Encoder not found for id: " + chosenId);
        }

        return "{" + chosenId + "}" + encoder.encode(rawPassword);
    }
}