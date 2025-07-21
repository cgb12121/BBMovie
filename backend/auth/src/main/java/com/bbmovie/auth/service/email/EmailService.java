package com.bbmovie.auth.service.email;

import java.time.ZonedDateTime;

public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken);

    void notifyChangedPassword(String receiver, ZonedDateTime time);

    void sendForgotPasswordEmail(String receiver, String resetToken);
}