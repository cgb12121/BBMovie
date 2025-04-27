package com.example.bbmovie.service.email;

import com.example.bbmovie.exception.CustomEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.extern.log4j.Log4j2;

import java.time.LocalTime;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String receiver, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Verify your email address");

            Context context = new Context();
            context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + verificationToken);
            context.setVariable("user", receiver);

            String htmlContent = templateEngine.process("verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", receiver);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send verification email!");
        }
    }

    @Override
    public void notifyChangedPassword(String receiver) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Notify on password change");

            Context context = new Context();
            context.setVariable("timeChanged", LocalTime.now().toString());

            String htmlContent = templateEngine.process("password-change", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Notify on password change sent to {}", receiver);

        } catch (MessagingException e) {
            log.error("Failed to send notify on password change email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send notify on password change email!");
        }
    }

    @Override
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(receiver);
            helper.setSubject("Verify your email address");

            Context context = new Context();
            context.setVariable("resetPasswordUrl", frontendUrl + "/reset-password?token=" + resetPasswordToken);
            context.setVariable("user", receiver);

            String htmlContent = templateEngine.process("reset-password", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Reset password email sent to {}", receiver);
        } catch (MessagingException e) {
            log.error("Failed to send Reset password email to {}: {}", receiver, e.getMessage());
            throw new CustomEmailException("Failed to send Reset password email!");
        }
    }
} 