package com.example.bbmovie.service.email.gmail;

import com.example.bbmovie.exception.CustomEmailException;
import com.example.bbmovie.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.extern.log4j.Log4j2;

import java.time.ZonedDateTime;

import static com.example.bbmovie.utils.EmailUtils.createResetPasswordEmailUrl;
import static com.example.bbmovie.utils.EmailUtils.createVerificationEmailUrl;

/*
 * This class is used to send emails to users.
 * It uses Spring Boot's JavaMailSender to send emails.
 * It uses Thymeleaf to generate the HTML content of the emails.
 * It uses the @Async annotation to send emails asynchronously.
 * 🛡️ Bonus: Don’t throw exception from Async method
 */
@Log4j2
@Service("gmailSmtp")
@RequiredArgsConstructor
public class GmailSmtpService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String CHAR_ENCODING_UTF_8 = "UTF-8";

    @Override
    @Async("emailExecutor")
    @Retryable(
            retryFor = {
                    MessagingException.class,
                    MailException.class,
                    CustomEmailException.class // must be thrown so that retry works
            },
            maxAttempts = 2,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendVerificationEmail(String receiver, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHAR_ENCODING_UTF_8);

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Verify your email address");

            Context context = new Context();
            context.setVariable("verificationUrl", createVerificationEmailUrl(frontendUrl, verificationToken));
            context.setVariable("user", receiver);
            String htmlContent = templateEngine.process("verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", receiver);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send verification email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send verification email!");
        }
    }

    @Override
    @Async("emailExecutor")
    @Retryable(
            retryFor = {
                    MessagingException.class,
                    MailException.class,
                    CustomEmailException.class
            },
            maxAttempts = 2,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void notifyChangedPassword(String receiver, ZonedDateTime timeChanged) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHAR_ENCODING_UTF_8);

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Notify on password change");

            Context context = new Context();
            context.setVariable("timeChanged", timeChanged.toString());
            String htmlContent = templateEngine.process("password-change", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Notify on password change sent to {}", receiver);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send notify on password change email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send notify on password change email!");
        }
    }

    @Override
    @Async("emailExecutor")
    @Retryable(
            retryFor = {
                    MessagingException.class,
                    MailException.class,
                    CustomEmailException.class
            },
            maxAttempts = 2,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHAR_ENCODING_UTF_8);

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Verify your email address");

            Context context = new Context();
            context.setVariable("resetPasswordUrl", createResetPasswordEmailUrl(frontendUrl, resetPasswordToken));
            context.setVariable("user", receiver);
            String htmlContent = templateEngine.process("reset-password", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Reset password email sent to {}", receiver);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send Reset password email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send Reset password email!");
        }
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverSendVerificationEmail(CustomEmailException e, String receiver, String verificationToken) {
        log.error("All retry attempts failed for sendVerificationEmail to {}: {}", receiver, e.getMessage());
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverNotifyChangedPassword(CustomEmailException e, String receiver) {
        log.error("All retry attempts failed for notifyChangedPassword to {}: {}", receiver, e.getMessage());
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverSendForgotPasswordEmail(CustomEmailException e, String receiver, String resetPasswordToken) {
        log.error("All retry attempts failed for sendForgotPasswordEmail to {}: {}", receiver, e.getMessage());
    }
} 