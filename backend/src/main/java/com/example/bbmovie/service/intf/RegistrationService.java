package com.example.bbmovie.service.intf;

import com.example.bbmovie.model.User;
import org.springframework.transaction.annotation.Transactional;
import com.example.bbmovie.dto.request.RegisterRequest;

import org.springframework.stereotype.Service;

@Service
public interface RegistrationService {
    @Transactional
    User registerUser(RegisterRequest request);

    @Transactional
    void verifyEmail(String token);

    void sendVerificationEmail(String email);
}
