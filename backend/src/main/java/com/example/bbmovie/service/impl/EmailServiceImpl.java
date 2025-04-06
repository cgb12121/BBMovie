package com.example.bbmovie.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.bbmovie.service.intf.EmailService;
import lombok.extern.log4j.Log4j2;

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
            context.setVariable("email", receiver);

            String htmlContent = templateEngine.process("verification", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", receiver);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", receiver, e.getMessage());
            throw new RuntimeException("Failed to send verification email!");
        }
    }
} 