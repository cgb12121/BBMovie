package com.bbmovie.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record CancelSubscriptionRequest(@NotNull Boolean immediate) {}


