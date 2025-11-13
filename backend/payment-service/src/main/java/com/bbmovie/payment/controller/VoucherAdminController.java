package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.VoucherCreateRequest;
import com.bbmovie.payment.dto.request.VoucherUpdateRequest;
import com.bbmovie.payment.dto.response.VoucherResponse;
import com.bbmovie.payment.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/vouchers")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class VoucherAdminController {

    private final VoucherService voucherService;

    @Autowired
    public VoucherAdminController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ApiResponse<VoucherResponse> create(@Valid @RequestBody VoucherCreateRequest req) {
        return ApiResponse.success(voucherService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> update(@PathVariable("id") UUID id, @Valid @RequestBody VoucherUpdateRequest req) {
        return ApiResponse.success(voucherService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        voucherService.delete(id);
        return ApiResponse.success(null, "Deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> get(@PathVariable("id") UUID id) {
        return ApiResponse.success(voucherService.get(id));
    }

    @GetMapping
    public ApiResponse<List<VoucherResponse>> list() {
        return ApiResponse.success(voucherService.listAll());
    }
}


