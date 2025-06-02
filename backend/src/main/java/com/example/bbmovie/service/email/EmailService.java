package com.example.bbmovie.service.email;

import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken);

    void notifyChangedPassword(String receiver, ZonedDateTime time);

    void sendForgotPasswordEmail(String receiver, String resetToken);
}