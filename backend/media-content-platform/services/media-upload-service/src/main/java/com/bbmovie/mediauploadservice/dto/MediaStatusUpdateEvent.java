package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaStatusUpdateEvent {
    private String uploadId;
    private MediaStatus status;
    private String reason;
    private String checksum;
    private Long fileSize;
    private String sparseChecksum;
    private Double duration;
    private String filePath;
    private List<String> availableResolutions;
    private String maxAvailableResolution;
    private Integer sourceWidth;
    private Integer sourceHeight;
}
