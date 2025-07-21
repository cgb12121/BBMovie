package com.bbmovie.auth.service.email.gmail;

import com.bbmovie.auth.exception.CustomEmailException;
import com.bbmovie.auth.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.extern.log4j.Log4j2;

import java.time.ZonedDateTime;

import static com.bbmovie.auth.utils.EmailUtils.createResetPasswordEmailUrl;
import static com.bbmovie.auth.utils.EmailUtils.createVerificationEmailUrl;

/*
 * This class is used to send emails to users.
 * It uses Spring Boot's JavaMailSender to send emails.
 * It uses Thymeleaf to generate the HTML content of the emails.
 * It uses the @Async annotation to send emails asynchronously.
 * 🛡️ Bonus: Don’t throw exception from Async method
 */
@Log4j2
@Service("gmailSmtp")
public class GmailSmtpService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String CHAR_ENCODING_UTF_8 = "UTF-8";

    @Autowired
    public GmailSmtpService(
            @Qualifier("gmailSmtpSender") JavaMailSender mailSender,
            TemplateEngine templateEngine
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    @Async("emailExecutor")
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
} 