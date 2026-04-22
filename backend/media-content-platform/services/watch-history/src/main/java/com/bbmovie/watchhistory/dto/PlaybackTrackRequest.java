package com.bbmovie.watchhistory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class PlaybackTrackRequest {

    @NotNull
    private UUID movieId;

    @NotNull
    @PositiveOrZero
    private Double positionSec;

    @PositiveOrZero
    private Double durationSec;

    private String eventType;

    private Boolean completed;

    private Map<String, Object> metadata;
}
