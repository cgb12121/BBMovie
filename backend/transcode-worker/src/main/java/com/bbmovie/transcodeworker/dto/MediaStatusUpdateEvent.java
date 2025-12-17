package com.bbmovie.transcodeworker.dto;

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
    private String status; // Using String to avoid Enum dependency coupling, or duplicate Enum
    private String reason;
    private String checksum;
    private Long fileSize;
    private String sparseChecksum;
}
