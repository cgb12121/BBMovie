package com.example.bbmovie.service.email.gmail;

import com.example.bbmovie.service.email.EmailService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

import static com.example.bbmovie.utils.EmailUtils.createResetPasswordEmailUrl;
import static com.example.bbmovie.utils.EmailUtils.createVerificationEmailUrl;

@Log4j2
@Service("gmailApi")
public class GmailApiService implements EmailService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final TemplateEngine templateEngine;

    @Autowired
    public GmailApiService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        try {
            Context context = new Context();
            context.setVariable("verificationUrl", createVerificationEmailUrl(frontendUrl, verificationToken));
            context.setVariable("user", to);
            String htmlContent = templateEngine.process("verification", context);

            MimeMessage email = createEmail(to, fromEmail, "Verify your email address", htmlContent);
            Message message = createMessageWithEmail(email);
            sendMessageWithGmailApi(message);
        } catch (MessagingException | IOException | GeneralSecurityException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void notifyChangedPassword(String receiver, ZonedDateTime timeChanged) {
        try {
            Context context = new Context();
            context.setVariable("timeChanged", timeChanged.toString());
            String htmlContent = templateEngine.process("password-change", context);

            MimeMessage email = createEmail(receiver, fromEmail, "Notify on password change", htmlContent);
            Message message = createMessageWithEmail(email);
            sendMessageWithGmailApi(message);
        } catch (MessagingException | IOException | GeneralSecurityException e) {
            log.error("Failed to send notify on password change email to {}: {}", receiver, e.getMessage());
        }
    }

    @Override
    public void sendForgotPasswordEmail(String receiver, String resetPasswordToken) {
        try {
            Context context = new Context();
            context.setVariable("resetPasswordUrl", createResetPasswordEmailUrl(frontendUrl, resetPasswordToken));
            context.setVariable("user", receiver);
            String htmlContent = templateEngine.process("reset-password", context);

            MimeMessage email = createEmail(receiver, fromEmail, "Verify your email address", htmlContent);
            Message message = createMessageWithEmail(email);
            sendMessageWithGmailApi(message);
        }  catch (MessagingException |IOException | GeneralSecurityException e) {
            log.error("Failed to send Reset password email to {}: {}", receiver, e.getMessage());
        }
    }

    private static MimeMessage createEmail(
            String toEmailAddress, String fromEmailAddress,
            String subject, String bodyHtml
    ) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmailAddress));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmailAddress));
        email.setSubject(subject);
        email.setContent(bodyHtml, "text/html; charset=utf-8");
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private void sendMessageWithGmailApi(Message message) throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(GmailScopes.GMAIL_SEND);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        Gmail service = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("BBMovie").build();

        service.users().messages().send("me", message).execute();
    }

}
