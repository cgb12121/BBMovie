package com.bbmovie.transcodeworker.service.validation.encode;

import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;

import java.nio.file.Path;

/**
 * VVS contract for encoded-output validation.
 */
public interface EncodeValidationService {

    ValidationReport validate(String uploadId, Path encodedFile, String renditionSuffix, EncodingExpectations expectations);
}
