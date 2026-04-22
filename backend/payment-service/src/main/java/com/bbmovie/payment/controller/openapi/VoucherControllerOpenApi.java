package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.response.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@SuppressWarnings("unused")
@Tag(name = "Vouchers", description = "User voucher APIs")
public interface VoucherControllerOpenApi {
    @Operation(summary = "Get my vouchers", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VoucherResponse>> myVouchers(@RequestHeader("Authorization") String bearer);
}

