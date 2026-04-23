package bbmovie.auth.mfa_service.service;

import bbmovie.auth.mfa_service.domain.MfaSecretEntity;
import bbmovie.auth.mfa_service.domain.OtpChallengeEntity;
import bbmovie.auth.mfa_service.repository.MfaSecretRepository;
import bbmovie.auth.mfa_service.repository.OtpChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MfaService {
    private final MfaSecretRepository secretRepository;
    private final OtpChallengeRepository otpRepository;
    private final TotpAlgorithmService totp;

    @Value("${mfa.otp-expiration-seconds:60}")
    private long otpExpirationSeconds;

    @Transactional
    public Map<String, Object> setup(String userId, String email) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        MfaSecretEntity secretEntity = secretRepository.findByUserId(userId).orElseGet(MfaSecretEntity::new);
        secretEntity.setUserId(userId);
        secretEntity.setEmail(email);
        secretEntity.setTotpSecret(totp.generateSecret());
        secretEntity.setMfaEnabled(false);
        secretEntity.setUpdatedAt(Instant.now());
        secretRepository.save(secretEntity);

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        OtpChallengeEntity challenge = new OtpChallengeEntity();
        challenge.setUserId(userId);
        challenge.setCode(otpCode);
        challenge.setUsed(false);
        challenge.setExpiresAt(Instant.now().plusSeconds(otpExpirationSeconds));
        otpRepository.save(challenge);

        String qrUri = "otpauth://totp/BBMovie:" + email + "?secret=" + secretEntity.getTotpSecret() + "&issuer=BBMovie";
        return Map.of(
                "secret", secretEntity.getTotpSecret(),
                "qrCode", qrUri,
                "otpCode", otpCode
        );
    }

    @Transactional
    public void verifySetupOtp(String userId, String otpCode) {
        OtpChallengeEntity challenge = otpRepository.findByCode(otpCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"));
        if (challenge.isUsed() || challenge.getExpiresAt().isBefore(Instant.now()) || !challenge.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
        MfaSecretEntity secret = secretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MFA setup not found"));
        secret.setMfaEnabled(true);
        secret.setUpdatedAt(Instant.now());
        challenge.setUsed(true);
        secretRepository.save(secret);
        otpRepository.save(challenge);
    }

    public boolean verifyTotp(String userId, String code) {
        MfaSecretEntity secret = secretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MFA setup not found"));
        return secret.isMfaEnabled() && totp.verifyCode(secret.getTotpSecret(), code);
    }

    @Transactional
    public String generateOtp(String userId) {
        otpRepository.deleteByExpiresAtBefore(Instant.now());
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        OtpChallengeEntity challenge = new OtpChallengeEntity();
        challenge.setId(UUID.randomUUID().toString());
        challenge.setUserId(userId);
        challenge.setCode(code);
        challenge.setExpiresAt(Instant.now().plusSeconds(otpExpirationSeconds));
        challenge.setUsed(false);
        otpRepository.save(challenge);
        return code;
    }

    @Transactional
    public boolean verifyOtp(String userId, String code) {
        OtpChallengeEntity challenge = otpRepository.findByCode(code).orElse(null);
        if (challenge == null || challenge.isUsed() || challenge.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        if (!challenge.getUserId().equals(userId)) {
            return false;
        }
        challenge.setUsed(true);
        otpRepository.save(challenge);
        return true;
    }
}
