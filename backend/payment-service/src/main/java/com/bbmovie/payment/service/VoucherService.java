package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.VoucherCreateRequest;
import com.bbmovie.payment.dto.request.VoucherUpdateRequest;
import com.bbmovie.payment.dto.response.VoucherResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface VoucherService {
    @Transactional
    VoucherResponse create(VoucherCreateRequest req);

    @Transactional
    VoucherResponse update(UUID id, VoucherUpdateRequest req);

    @Transactional
    void delete(UUID id);

    @Transactional(readOnly = true)
    VoucherResponse get(UUID id);

    @Transactional(readOnly = true)
    List<VoucherResponse> listAll();

    @Transactional(readOnly = true)
    VoucherResponse check(String code, String userId);

    @Transactional
    void markUsed(String code, String userId);

    @Transactional(readOnly = true)
    List<VoucherResponse> listAvailableForUser(String userId);
}
