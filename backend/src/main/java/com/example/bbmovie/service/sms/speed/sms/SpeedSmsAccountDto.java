package com.example.bbmovie.service.sms.speed.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedSmsAccountDto {
    String email;
    BigDecimal balance;
    String currency;
}