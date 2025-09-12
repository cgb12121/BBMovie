package com.bbmovie.payment.dto.request;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaxRateRequest {
    @Nullable private String zip;
    @Nullable private Boolean useClientIp;
    @Nullable private String street;
    @Nullable private String state;
    @Nullable private String ipAddress;
    @Nullable private String country;
    @Nullable private String city;
}
