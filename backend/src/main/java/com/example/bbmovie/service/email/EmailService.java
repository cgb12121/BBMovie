package com.example.bbmovie.service.email;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken);

    void notifyChangedPassword(String receiver);

    void sendForgotPasswordEmail(String receiver, String resetToken);
}