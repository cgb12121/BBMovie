package bbmovie.auth.mfa_service.service;

import bbmovie.auth.mfa_service.dto.request.GenerateOtpRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyMfaRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyOtpRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyTotpRequest;
import bbmovie.auth.mfa_service.dto.response.MfaSetupResponse;
import bbmovie.auth.mfa_service.service.MfaStateStore.MfaState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MfaMigrationService {

    private final TotpService totpService;
    private final OtpService otpService;
    private final MfaStateStore mfaStateStore;

    public MfaMigrationService(TotpService totpService, OtpService otpService, MfaStateStore mfaStateStore) {
        this.totpService = totpService;
        this.otpService = otpService;
        this.mfaStateStore = mfaStateStore;
    }

    public MfaSetupResponse setup(String authorizationHeader) {
        String email = extractEmailFromAuthorization(authorizationHeader);
        MfaState state = mfaStateStore.getOrCreate(email);
        if (state.enabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA already enabled");
        }

        TotpService.SetupData setupData = totpService.generateSetupData(email);
        mfaStateStore.save(email, new MfaState(setupData.secret(), false));
        otpService.generate(email);

        return new MfaSetupResponse(true, setupData.secret(), setupData.qrCode());
    }

    public void verify(String authorizationHeader, VerifyMfaRequest request) {
        String email = extractEmailFromAuthorization(authorizationHeader);
        MfaState state = mfaStateStore.getOrCreate(email);
        if (state.totpSecret() == null || state.totpSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA not initialized for this user");
        }

        boolean validOtp = otpService.verifyAndConsume(email, request.code());
        if (!validOtp) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        mfaStateStore.save(email, new MfaState(state.totpSecret(), true));
    }

    public boolean verifyTotp(VerifyTotpRequest request) {
        MfaState state = mfaStateStore.getOrCreate(request.email());
        if (!state.enabled() || state.totpSecret() == null || state.totpSecret().isBlank()) {
            return false;
        }
        return totpService.verifyCode(request.totpCode(), state.totpSecret());
    }

    public String generateOtp(GenerateOtpRequest request) {
        return otpService.generate(request.email());
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        return otpService.verifyAndConsume(request.email(), request.otp());
    }

    private String extractEmailFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
        return token;
    }
}
