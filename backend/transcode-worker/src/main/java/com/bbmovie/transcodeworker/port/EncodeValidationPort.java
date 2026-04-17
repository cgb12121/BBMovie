package com.bbmovie.transcodeworker.port;

import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;

import java.nio.file.Path;

public interface EncodeValidationPort {

    ValidationReport validate(String uploadId, Path encodedFile, String renditionSuffix, EncodingExpectations expectations);
}
