package com.example.bbmovie.service.intf;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken);

    void notifyChangedPassword(String receiver);
}