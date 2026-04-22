package com.bbmovie.watchhistory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeState(
        double positionSec,
        double durationSec,
        long updatedAtEpochSec,
        boolean completed) {
}
