package com.example.bbmovie.service.sms.speed.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedSmsSendDto {
    private long tranId;
    private int totalSMS;
    private int totalPrice;
    private List<String> invalidPhone;
}