package com.bbmovie.transcodeworker.port;

import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;

public interface ComplexityAnalysisPort {

    ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata);
}
