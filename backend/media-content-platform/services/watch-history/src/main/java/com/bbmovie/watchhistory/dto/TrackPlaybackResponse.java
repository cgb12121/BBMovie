package com.bbmovie.watchhistory.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrackPlaybackResponse {
    String status;
    boolean resumeCached;
    boolean analyticsEventPublished;
    long nextTrackAtEpochSec;
}
