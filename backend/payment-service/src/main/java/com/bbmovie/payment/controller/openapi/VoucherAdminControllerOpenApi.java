package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.VoucherCreateRequest;
import com.bbmovie.payment.dto.request.VoucherUpdateRequest;
import com.bbmovie.payment.dto.response.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Voucher Admin", description = "Admin APIs for voucher management")
public interface VoucherAdminControllerOpenApi {
    @Operation(summary = "Create voucher", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VoucherResponse> create(@Valid @RequestBody VoucherCreateRequest req);

    @Operation(summary = "Update voucher", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VoucherResponse> update(@PathVariable("id") UUID id, @Valid @RequestBody VoucherUpdateRequest req);

    @Operation(summary = "Delete voucher", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<Void> delete(@PathVariable("id") UUID id);

    @Operation(summary = "Get voucher by ID", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<VoucherResponse> get(@PathVariable("id") UUID id);

    @Operation(summary = "List vouchers", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<VoucherResponse>> list();
}

