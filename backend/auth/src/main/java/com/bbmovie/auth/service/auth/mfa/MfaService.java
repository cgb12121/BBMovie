package com.bbmovie.auth.service.auth.mfa;

import com.bbmovie.auth.dto.request.MfaVerifyRequest;
import com.bbmovie.auth.dto.response.MfaSetupResult;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.service.auth.verify.otp.OtpService;
import com.bbmovie.auth.service.nats.TotpProducer;
import com.example.common.enums.NotificationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Log4j2
@Service
public class MfaService {

    private final JoseProvider joseProvider;
    private final TotpService totpService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final TotpProducer totpProducer;

    @Autowired
    public MfaService(
            JoseProvider joseProvider, TotpService totpService,
            UserRepository userRepository, OtpService otpService, TotpProducer totpProducer
    ) {
        this.joseProvider = joseProvider;
        this.totpService = totpService;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.totpProducer = totpProducer;
    }

    public MfaSetupResult getMfaSetupResult(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Map<String, Object> claims = joseProvider.getClaimsFromToken(token);
        String email = String.valueOf(claims.get(SUB));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA already enabled");
        }

        MfaSetupResult result = totpService.generateSetupData(email);
        user.setTotpSecret(result.secret());
        user.setMfaEnabled(false);
        userRepository.save(user);

        String otp = otpService.generateOtpToken(user);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            totpProducer.sendNotification(String.valueOf(user.getId()), NotificationType.EMAIL, user.getEmail(),
                    "Your MFA setup confirmation code is: " + otp);
        }
        return result;
    }

    public void verify(String authorizationHeader, MfaVerifyRequest request) {
        String token = extractBearerToken(authorizationHeader);
        Map<String, Object> claims = joseProvider.getClaimsFromToken(token);
        String emailFromToken = String.valueOf(claims.get(SUB));

        String emailFromOtp = otpService.getEmailForOtpToken(request.code());
        if (!emailFromToken.equals(emailFromOtp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(emailFromToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA not initialized for this user");
        }

        otpService.deleteOtpToken(request.code());

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    private String extractBearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
        return header.substring(7);
    }
}
