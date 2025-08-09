package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.PaymentRequestDto;
import com.bbmovie.payment.service.vnpay.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class Test {

    @PostMapping("/vnpay")
    public String vnpay(HttpServletRequest httpServletRequest) {
        VNPayConfig  c = new VNPayConfig();
        return c.createOrder(httpServletRequest, 1000, "billpayment", "https://abc123.ngrok.io/api/payment/vnpay/return");
    }
}
