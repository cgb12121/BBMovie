package com.bbmovie.dto.request;

import com.bbmovie.entity.enums.WatchStatus;

import java.util.UUID;

public record UpsertItemRequest(UUID movieId, WatchStatus status, String notes) {}
