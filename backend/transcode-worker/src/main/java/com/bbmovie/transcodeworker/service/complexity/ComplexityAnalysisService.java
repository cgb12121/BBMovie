package com.bbmovie.transcodeworker.service.complexity;

import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;

/**
 * CAS contract for complexity analysis.
 */
public interface ComplexityAnalysisService {

    ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata);
}
