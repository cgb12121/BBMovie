package com.bbmovie.email.service.email.mailsender;

import com.bbmovie.email.exception.CustomEmailException;
import com.bbmovie.email.service.email.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZonedDateTime;

import static com.bbmovie.email.utils.EmailUtils.createResetPasswordEmailUrl;
import static com.bbmovie.email.utils.EmailUtils.createVerificationEmailUrl;

@Log4j2
@Service("mailersendSmtp")
public class MailerSenderSmtpService implements EmailService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final TemplateEngine templateEngine;
    private final JavaMailSender javaMailSender;

    @Autowired
    public MailerSenderSmtpService(
            @Qualifier("mailersendSmtpSender") JavaMailSender javaMailSender,
            TemplateEngine templateEngine
    ) {
        this.templateEngine = templateEngine;
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        Context context = new Context();
        context.setVariable("verificationUrl", createVerificationEmailUrl(frontendUrl, verificationToken));
        context.setVariable("user", to);
        String htmlContent = templateEngine.process("verification", context);

        sendEmail(to, "Verify Your Email", htmlContent);
    }

    @Override
    public void notifyChangedPassword(String receiver, ZonedDateTime timeChanged) {
        Context context = new Context();
        context.setVariable("timeChanged", timeChanged.toString());
        String htmlContent = templateEngine.process("password-change", context);

        sendEmail(receiver, "Password Changed", htmlContent);
    }

    @Override
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        Context context = new Context();
        context.setVariable("resetPasswordUrl", createResetPasswordEmailUrl(frontendUrl, resetPasswordToken));
        context.setVariable("user", receiver);
        String htmlContent = templateEngine.process("reset-password", context);

        sendEmail(receiver, "Reset Your Password", htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML content

            javaMailSender.send(mimeMessage);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new CustomEmailException("Failed to send email");
        }
    }
}
