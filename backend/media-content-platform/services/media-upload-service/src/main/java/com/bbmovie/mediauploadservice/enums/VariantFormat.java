package com.bbmovie.mediauploadservice.enums;

public enum VariantFormat {
    MP4,
    HLS;

    public boolean isAdaptive() {
        return this == HLS;
    }
}
