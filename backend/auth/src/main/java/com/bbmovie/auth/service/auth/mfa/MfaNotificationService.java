package com.bbmovie.auth.service.auth.mfa;

import com.bbmovie.auth.service.nats.TotpProducer;
import com.example.common.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MfaNotificationService {

    private final TotpProducer totpProducer;
    private final TotpService totpService;

    @Autowired
    public MfaNotificationService(TotpProducer totpProducer, TotpService totpService) {
        this.totpProducer = totpProducer;
        this.totpService = totpService;
    }

    public void sendRecoveryCode(String userId, String secret, String email, String phone) {
        String code = totpService.generateCurrentCode(secret);
        if (email != null && !email.isBlank()) {
            totpProducer.sendNotification(userId, NotificationType.EMAIL, email,
                    "Your 2FA verification code is: " + code);
        }
        if (phone != null && !phone.isBlank()) {
            totpProducer.sendNotification(userId, NotificationType.SMS, phone,
                    "Your 2FA code: " + code);
        }
    }
}