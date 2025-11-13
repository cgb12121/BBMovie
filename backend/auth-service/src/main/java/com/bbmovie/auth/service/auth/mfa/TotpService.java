package com.bbmovie.auth.service.auth.mfa;

import com.bbmovie.auth.dto.response.MfaSetupResult;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class TotpService {

    private final CodeVerifier codeVerifier;
    private final CodeGenerator codeGenerator;
    private final TimeProvider timeProvider;
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;

    @Value("${totp.qr.issuer}")
    private String issuer;

    @Autowired
    public TotpService(
            CodeVerifier codeVerifier, CodeGenerator codeGenerator,
            TimeProvider timeProvider, SecretGenerator secretGenerator,
            QrGenerator qrGenerator
    ) {
        this.codeVerifier = codeVerifier;
        this.codeGenerator = codeGenerator;
        this.timeProvider = timeProvider;
        this.secretGenerator = secretGenerator;
        this.qrGenerator = qrGenerator;
    }

    public boolean verifyCode(String code, String secret) {
        return codeVerifier.isValidCode(secret, code);
    }

    public String generateCurrentCode(String secret) {
        try {
            long timeIndex = timeProvider.getTime() / 30;
            return codeGenerator.generate(secret, timeIndex);
        } catch (CodeGenerationException e) {
            log.error("Failed to generate code", e);
            throw new IllegalStateException("Failed to generate code.");
        }
    }

    public MfaSetupResult generateSetupData(String email) {
        String secret = secretGenerator.generate();
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA256)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] qrImage = qrGenerator.generate(qrData);
            String dataUri = "data:" +
                    qrGenerator.getImageMimeType() +
                    ";base64," +
                    Base64.getEncoder().encodeToString(qrImage);
            return new MfaSetupResult(secret, dataUri);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}