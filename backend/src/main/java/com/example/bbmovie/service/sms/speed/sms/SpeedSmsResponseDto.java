package com.example.bbmovie.service.sms.speed.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedSmsResponseDto {
    private String status;
    private String code;
    private Object data;
    private String message;
}