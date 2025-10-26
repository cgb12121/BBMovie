package com.bbmovie.auth.service.auth.registration;

import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.entity.User;
import org.springframework.transaction.annotation.Transactional;

public interface RegistrationService {
    @Transactional
    void register(RegisterRequest request);

    @Transactional
    String verifyAccountByEmail(String token);

    void sendVerificationEmail(String email);

    @Transactional
    void verifyAccountByOtp(String otp);

    void sendOtp(User user);
}
