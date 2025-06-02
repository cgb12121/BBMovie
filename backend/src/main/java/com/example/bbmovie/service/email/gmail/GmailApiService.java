package com.example.bbmovie.service.email.gmail;

import com.example.bbmovie.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZonedDateTime;

@Log4j2
@Service("gmailApi")
@RequiredArgsConstructor
public class GmailApiService implements EmailService {

    @Value("${mail-sender.token}")
    private String mailSenderToken;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final TemplateEngine templateEngine;

    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        Context context = new Context();
        context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + verificationToken);
        context.setVariable("user", to);
        String htmlContent = templateEngine.process("verification", context);
    }

    @Override
    public void notifyChangedPassword(String receiver, ZonedDateTime timeChanged) {
        Context context = new Context();
        context.setVariable("timeChanged", timeChanged.toString());

        String htmlContent = templateEngine.process("password-change", context);
    }

    @Override
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        Context context = new Context();
        context.setVariable("resetPasswordUrl", frontendUrl + "/reset-password?token=" + resetPasswordToken);
        context.setVariable("user", receiver);
        String htmlContent = templateEngine.process("reset-password", context);
    }
}
