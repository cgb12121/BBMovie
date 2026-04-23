package bbmovie.auth.identity_service.service;

import bbmovie.auth.identity_service.domain.Role;
import bbmovie.auth.identity_service.domain.UserEntity;
import bbmovie.auth.identity_service.dto.ChangePasswordRequest;
import bbmovie.auth.identity_service.dto.ForgotPasswordRequest;
import bbmovie.auth.identity_service.dto.InternalVerifyCredentialsResponse;
import bbmovie.auth.identity_service.dto.RegisterRequest;
import bbmovie.auth.identity_service.dto.ResetPasswordRequest;
import bbmovie.auth.identity_service.dto.SendVerificationEmailRequest;
import bbmovie.auth.identity_service.dto.VerifyCredentialsRequest;
import bbmovie.auth.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserRepository userRepository;
    private final TokenStoreService tokenStoreService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password and confirm password do not match");
        }
        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setDisplayedUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(Role.USER);
        user.setEnabled(false);
        userRepository.save(user);
        return tokenStoreService.createVerificationToken(user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = tokenStoreService.getVerificationEmail(token);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification token");
        }
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        tokenStoreService.deleteVerificationToken(token);
    }

    public String sendVerification(SendVerificationEmailRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already verified");
        }
        return tokenStoreService.createVerificationToken(user.getEmail());
    }

    @Transactional
    public void changePassword(String principalEmail, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByDisplayedUsername(principalEmail)
                .or(() -> userRepository.findByEmail(principalEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirm password do not match");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password can not be the same as current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        return userRepository.findByEmail(request.email())
                .map(UserEntity::getEmail)
                .map(tokenStoreService::createResetToken)
                .orElse("noop");
    }

    @Transactional
    public void resetPassword(String token, ResetPasswordRequest request) {
        String email = tokenStoreService.getResetEmail(token);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirm password do not match");
        }
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        tokenStoreService.deleteResetToken(token);
    }

    public InternalVerifyCredentialsResponse verifyCredentials(VerifyCredentialsRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not enabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new InternalVerifyCredentialsResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.isMfaEnabled()
        );
    }
}
