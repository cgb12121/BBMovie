package com.bbmovie.transcodeworker.port;

import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;

import java.nio.file.Path;

public interface VideoQualityPort {

    QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix);
}
