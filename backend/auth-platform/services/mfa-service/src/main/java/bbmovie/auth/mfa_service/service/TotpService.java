package bbmovie.auth.mfa_service.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TotpService {

    private final CodeVerifier codeVerifier;
    private final String issuer;

    public TotpService(@Value("${totp.qr.issuer:BBMovie}") String issuer) {
        this.codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA256), new SystemTimeProvider());
        this.issuer = issuer;
    }

    public SetupData generateSetupData(String email) {
        String secret = new DefaultSecretGenerator().generate();
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA256)
                .digits(6)
                .period(30)
                .build();

        String otpauth = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                urlEncode(issuer), urlEncode(email), qrData.getSecret(), urlEncode(issuer), "SHA256", 6, 30
        );
        String qrPayload = Base64.getEncoder().encodeToString(otpauth.getBytes(StandardCharsets.UTF_8));
        String dataUri = "data:text/plain;base64," + qrPayload;

        return new SetupData(secret, dataUri);
    }

    public boolean verifyCode(String code, String secret) {
        return codeVerifier.isValidCode(secret, code);
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20");
    }

    public record SetupData(String secret, String qrCode) {
    }
}
