package com.bbmovie.auth.config;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.NtpTimeProvider;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class TotpConfig {

    @Value("${totp.digits:6}")
    private int digits;

    @Value("${totp.time-period:60}")
    private int period;

    @Value("${totp.algorithm:SHA256}")
    private String algorithm;

    @Value("${totp.ntp.enabled:false}")
    private boolean ntpEnabled;

    @Value("${totp.ntp.server:pool.ntp.org}")
    private String ntpServer;

    @Bean
    public SecretGenerator secretGenerator() {
        return new DefaultSecretGenerator(digits);
    }

    @Bean
    public QrGenerator qrGenerator() {
        return new ZxingPngQrGenerator();
    }

    @Bean
    public TimeProvider timeProvider() {
        if (ntpEnabled) {
            try {
                return new NtpTimeProvider(ntpServer);
            } catch (Exception e) {
                log.error("NTP failed, falling back to system time: {}", e.getMessage());
                return new SystemTimeProvider();
            }
        }
        return new SystemTimeProvider();
    }

    @Bean
    public CodeGenerator codeGenerator() {
        return new DefaultCodeGenerator(getHashingAlgorithm(), digits);
    }

    @Bean
    public CodeVerifier codeVerifier() {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator(), timeProvider());
        verifier.setTimePeriod(period);
        return verifier;
    }

    private HashingAlgorithm getHashingAlgorithm() {
        return switch (algorithm.trim().toUpperCase()) {
            case "SHA256" -> HashingAlgorithm.SHA256;
            case "SHA512" -> HashingAlgorithm.SHA512;
            default -> HashingAlgorithm.SHA256; // avoid SHA1
        };
    }
}