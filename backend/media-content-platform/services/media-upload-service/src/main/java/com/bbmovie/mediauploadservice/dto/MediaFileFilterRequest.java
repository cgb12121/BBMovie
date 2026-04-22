package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MediaFileFilterRequest {
    private String search; // search by filename or uploadId
    private MediaStatus status;
    private UploadPurpose purpose;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String userId;
}
