package com.example.bbmovie.controller;

import com.example.bbmovie.dto.request.SmsRequest;
import com.example.bbmovie.service.sms.twillio.TwilioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/twilio")
public class TwilioSmsController {

    private final TwilioService twilioService;

    @Autowired
    public TwilioSmsController(TwilioService twilioService) {
        this.twilioService = twilioService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendSmsViaTwilio(@RequestBody SmsRequest smsRequest) {
        try {
            String messageSid = twilioService.sendSms(smsRequest.getToPhoneNumber(), smsRequest.getMessage());
            return ResponseEntity.ok("SMS sent successfully. Message SID: " + messageSid);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send SMS: " + e.getMessage());
        }
    }
}