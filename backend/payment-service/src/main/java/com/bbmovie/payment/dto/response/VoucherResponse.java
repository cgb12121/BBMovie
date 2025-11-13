package com.bbmovie.payment.dto.response;

import com.bbmovie.payment.entity.enums.VoucherType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoucherResponse {
    private UUID id;
    private String code;
    private VoucherType type;
    private BigDecimal percentage;
    private BigDecimal amount;
    private String userSpecificId;
    private boolean permanent;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int maxUsePerUser;
    private boolean active;
}


