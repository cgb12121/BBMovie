package com.bbmovie.transcodeworker.service.validation.encode.dto;

/**
 * Expected encoded attributes for VVS checks.
 */
public record EncodingExpectations(
        String expectedCodec,
        Integer expectedWidth,
        Integer expectedHeight,
        Long minBitrate,
        Long maxBitrate,
        String expectedAudioCodec
) {
}
