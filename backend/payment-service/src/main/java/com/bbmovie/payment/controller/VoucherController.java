package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.response.VoucherResponse;
import com.bbmovie.payment.service.VoucherService;
import com.bbmovie.payment.utils.SimpleJwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voucher")
public class VoucherController {

    private final VoucherService voucherService;

    @Autowired
    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/my")
    public ApiResponse<java.util.List<VoucherResponse>> myVouchers(@RequestHeader("Authorization") String bearer) {
        String userId = SimpleJwtDecoder.getUserId(bearer);
        return ApiResponse.success(voucherService.listAvailableForUser(userId));
    }
}
