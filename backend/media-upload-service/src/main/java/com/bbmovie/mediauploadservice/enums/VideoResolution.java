package com.bbmovie.mediauploadservice.enums;

public enum VideoResolution {
    P144(256, 144),
    P360(640, 360),
    P720(1280, 720),
    P1080(1920, 1080);

    public final int width;
    public final int height;

    VideoResolution(int w, int h) {
        this.width = w;
        this.height = h;
    }
}
