package com.example.bbmovie.service.email.mailsender;

import com.example.bbmovie.service.email.EmailService;
import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.Recipient;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZonedDateTime;

import static com.example.bbmovie.constant.Domain.DOMAIN_NAME;
import static com.example.bbmovie.utils.EmailUtils.createResetPasswordEmailUrl;
import static com.example.bbmovie.utils.EmailUtils.createVerificationEmailUrl;

@Log4j2
@Service("mailersendApi")
@RequiredArgsConstructor
public class MailerSenderApiService implements EmailService {

    @Value("${mail-sender.token}")
    private String mailSenderToken;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final TemplateEngine templateEngine;

    /**
     *    instead of using Thymeleaf engine to process the template, we
     *    can be replaced by email.setTemplateId("Your MailerSend template ID");
     */
    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        try {
            Context context = new Context();
            context.setVariable("verificationUrl", createVerificationEmailUrl(frontendUrl, verificationToken));
            context.setVariable("user", to);
            String htmlContent = templateEngine.process("verification", context);

            Email email = new Email();
            email.setFrom(DOMAIN_NAME, fromEmail);
            email.AddRecipient(new Recipient(to, to));
            email.setSubject("Verify your email address");
            email.setHtml(htmlContent);

            MailerSend mailerSend = new MailerSend();
            mailerSend.setToken(mailSenderToken);

            MailerSendResponse response = mailerSend.emails().send(email);
            log.info("Verification email sent: {}", response);
        } catch (MailerSendException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Fatal error when sending verification email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void notifyChangedPassword(String receiver, ZonedDateTime timeChanged) {
        try {
            Context context = new Context();
            context.setVariable("timeChanged", timeChanged.toString());
            String htmlContent = templateEngine.process("password-change", context);

            Email email = new Email();
            email.setFrom(DOMAIN_NAME, fromEmail);
            email.AddRecipient(new Recipient(receiver, receiver));
            email.setSubject("Notify on password change");
            email.setHtml(htmlContent);

            MailerSend mailerSend = new MailerSend();
            mailerSend.setToken(mailSenderToken);

            MailerSendResponse response = mailerSend.emails().send(email);
            log.info("Notify on password change sent: {}", response);
        } catch (MailerSendException e) {
            log.error("Failed to send notify on password change email to {}: {}", receiver, e.getMessage());
        } catch (Exception e) {
            log.error("Fatal error when sending notify on password change email to {}: {}", receiver, e.getMessage());
        }
    }

    @Override
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        try {
            Context context = new Context();
            context.setVariable("resetPasswordUrl", createResetPasswordEmailUrl(frontendUrl, resetPasswordToken));
            context.setVariable("user", receiver);
            String htmlContent = templateEngine.process("reset-password", context);

            Email email = new Email();
            email.setFrom(DOMAIN_NAME, fromEmail);
            email.AddRecipient(new Recipient(receiver, receiver));
            email.setSubject("Verify your email address");
            email.setHtml(htmlContent);

            MailerSend mailerSend = new MailerSend();
            mailerSend.setToken(mailSenderToken);

            MailerSendResponse response = mailerSend.emails().send(email);
            log.info("Reset password email sent: {}", response);
        } catch (MailerSendException e) {
            log.error("Failed to send Reset password email to {}: {}", receiver, e.getMessage());
        } catch (Exception e) {
            log.error("Fatal error when sending Reset password email to {}: {}", receiver, e.getMessage());
        }
    }
}
