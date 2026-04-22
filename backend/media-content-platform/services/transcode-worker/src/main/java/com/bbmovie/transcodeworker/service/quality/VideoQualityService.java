package com.bbmovie.transcodeworker.service.quality;

import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;

import java.nio.file.Path;

/**
 * VQS contract for quality scoring.
 */
public interface VideoQualityService {

    QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix);
}
