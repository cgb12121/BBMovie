package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.VoucherCreateRequest;
import com.bbmovie.payment.dto.request.VoucherUpdateRequest;
import com.bbmovie.payment.dto.response.VoucherResponse;
import com.bbmovie.payment.entity.Voucher;
import com.bbmovie.payment.entity.VoucherRedemption;
import com.bbmovie.payment.entity.enums.VoucherType;
import com.bbmovie.payment.repository.VoucherRedemptionRepository;
import com.bbmovie.payment.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;

    @Autowired
    public VoucherService(VoucherRepository voucherRepository, VoucherRedemptionRepository redemptionRepository) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
    }

    // Admin
    @Transactional
    public VoucherResponse create(VoucherCreateRequest req) {
        validate(req.type(), req.percentage(), req.amount());
        Voucher v = Voucher.builder()
                .code(req.code())
                .type(req.type())
                .percentage(req.percentage())
                .amount(req.amount())
                .userSpecificId(req.userSpecificId())
                .permanent(req.permanent())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .maxUsePerUser(req.maxUsePerUser())
                .active(req.active())
                .build();
        v = voucherRepository.save(v);
        return toResponse(v);
    }

    @Transactional
    public VoucherResponse update(UUID id, VoucherUpdateRequest req) {
        validate(req.type(), req.percentage(), req.amount());
        Voucher v = voucherRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        v.setCode(req.code());
        v.setType(req.type());
        v.setPercentage(req.percentage());
        v.setAmount(req.amount());
        v.setUserSpecificId(req.userSpecificId());
        v.setPermanent(req.permanent());
        v.setStartAt(req.startAt());
        v.setEndAt(req.endAt());
        v.setMaxUsePerUser(req.maxUsePerUser());
        v.setActive(req.active());
        v = voucherRepository.save(v);
        return toResponse(v);
    }

    @Transactional
    public void delete(UUID id) {
        if (!voucherRepository.existsById(id)) {
            throw new IllegalArgumentException("Voucher not found");
        }
        voucherRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public VoucherResponse get(UUID id) {
        return voucherRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> listAll() {
        return voucherRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // User
    @Transactional(readOnly = true)
    public VoucherResponse check(String code, String userId) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        validateUsabilityForUser(v, userId);
        return toResponse(v);
    }

    @Transactional
    public void markUsed(String code, String userId) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        validateUsabilityForUser(v, userId);

        VoucherRedemption red = redemptionRepository.findByVoucherAndUserId(v, userId)
                .orElseGet(() -> VoucherRedemption.builder().voucher(v).userId(userId).usedCount(0).build());
        if (red.getId() == null) {
            red = redemptionRepository.save(red);
        }
        if (red.getUsedCount() >= v.getMaxUsePerUser()) {
            throw new IllegalStateException("Voucher usage limit reached for user");
        }
        red.setUsedCount(red.getUsedCount() + 1);
        redemptionRepository.save(red);
    }

    @Transactional(readOnly = true)
    public java.util.List<VoucherResponse> listAvailableForUser(String userId) {
        return voucherRepository.findAll().stream()
                .filter(Voucher::isActive)
                .filter(v -> v.getUserSpecificId() == null || v.getUserSpecificId().equals(userId))
                .filter(v -> {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    return v.isPermanent() ||
                            (v.getStartAt() == null || !now.isBefore(v.getStartAt())) &&
                                    (v.getEndAt() == null || !now.isAfter(v.getEndAt()));
                })
                .filter(v -> redemptionRepository.countByVoucherAndUserId(v, userId) < v.getMaxUsePerUser())
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private void validate(VoucherType type, java.math.BigDecimal percentage, java.math.BigDecimal amount) {
        if (type == VoucherType.PERCENTAGE) {
            if (percentage == null) throw new IllegalArgumentException("percentage required for PERCENTAGE type");
        } else if (type == VoucherType.FIXED_AMOUNT) {
            if (amount == null) throw new IllegalArgumentException("amount required for FIXED_AMOUNT type");
        }
    }

    private void validateUsabilityForUser(Voucher v, String userId) {
        if (!v.isActive()) {
            throw new IllegalStateException("Voucher inactive");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean timeOk = v.isPermanent() ||
                (v.getStartAt() == null || !now.isBefore(v.getStartAt())) &&
                        (v.getEndAt() == null || !now.isAfter(v.getEndAt()));
        if (!timeOk) {
            throw new IllegalStateException("Voucher expired or not started");
        }
        if (v.getUserSpecificId() != null && !v.getUserSpecificId().equals(userId)) {
            throw new IllegalStateException("Voucher not valid for this user");
        }
        long used = redemptionRepository.countByVoucherAndUserId(v, userId);
        if (used >= v.getMaxUsePerUser()) {
            throw new IllegalStateException("Voucher usage limit reached for user");
        }
    }

    private VoucherResponse toResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .type(v.getType())
                .percentage(v.getPercentage())
                .amount(v.getAmount())
                .userSpecificId(v.getUserSpecificId())
                .permanent(v.isPermanent())
                .startAt(v.getStartAt())
                .endAt(v.getEndAt())
                .maxUsePerUser(v.getMaxUsePerUser())
                .active(v.isActive())
                .build();
    }
}
