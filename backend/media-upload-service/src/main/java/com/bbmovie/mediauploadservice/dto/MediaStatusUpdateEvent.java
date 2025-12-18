package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStatusUpdateEvent {
    private String uploadId;
    private MediaStatus status;
    private String reason;
    private String checksum;
    private Long fileSize;
    private String sparseChecksum;
}
