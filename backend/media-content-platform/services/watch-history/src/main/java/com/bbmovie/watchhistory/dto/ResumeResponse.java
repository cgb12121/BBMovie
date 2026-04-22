package com.bbmovie.watchhistory.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ResumeResponse {
    UUID movieId;
    double positionSec;
    double durationSec;
    long updatedAtEpochSec;
    boolean completed;
}
