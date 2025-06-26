package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.service.sms.speed.sms.SpeedSmsResponseDto;
import com.example.bbmovie.service.sms.speed.sms.SpeedSmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/speed-sms")
public class SpeedSmsController {

    private final SpeedSmsService speedSmsService;

    @Autowired
    public SpeedSmsController(SpeedSmsService speedSmsService) {
        this.speedSmsService = speedSmsService;
    }

    @RequestMapping("/account/info")
    public ResponseEntity<ApiResponse<SpeedSmsResponseDto>> accountInfo() {
        return ResponseEntity.ok(ApiResponse.success(speedSmsService.getAccountInfo()));
    }

    @RequestMapping("/send")
    public ResponseEntity<ApiResponse<SpeedSmsResponseDto>> send(String to, String content, int messageType) {
        return ResponseEntity.ok(ApiResponse.success(speedSmsService.sendSms(to, content, messageType)));
    }
}
