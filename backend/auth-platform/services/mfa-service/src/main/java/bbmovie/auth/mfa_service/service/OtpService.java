package bbmovie.auth.mfa_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, OtpRecord> otpByCode = new ConcurrentHashMap<>();
    private final long otpTtlSeconds;

    public OtpService(@Value("${app.otp-expiration-seconds:300}") long otpTtlSeconds) {
        this.otpTtlSeconds = otpTtlSeconds;
    }

    public String generate(String email) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(otpTtlSeconds);
        otpByCode.put(otp, new OtpRecord(email, expiresAt));
        return otp;
    }

    public boolean verifyAndConsume(String email, String otp) {
        OtpRecord record = otpByCode.get(otp);
        if (record == null || !record.email().equalsIgnoreCase(email) || record.expiresAt().isBefore(Instant.now())) {
            return false;
        }
        otpByCode.remove(otp);
        return true;
    }

    private record OtpRecord(String email, Instant expiresAt) {
    }
}
