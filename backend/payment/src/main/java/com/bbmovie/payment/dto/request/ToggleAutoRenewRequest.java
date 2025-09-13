package com.bbmovie.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record ToggleAutoRenewRequest(@NotNull Boolean autoRenew) {}


