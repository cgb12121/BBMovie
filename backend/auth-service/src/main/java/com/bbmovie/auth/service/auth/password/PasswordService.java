package com.bbmovie.auth.service.auth.password;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.exception.CustomEmailException;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordService {
    @Transactional(noRollbackFor = CustomEmailException.class)
    void changePassword(String requestEmail, ChangePasswordRequest request);

    void sendForgotPasswordEmail(String email);

    void resetPassword(String token, ResetPasswordRequest request);
}
