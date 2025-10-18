package com.bbmovie.fileservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiskUsageResponse {
    private String path;
    private String totalSpace;
    private String usableSpace;
    private String freeSpace;
    private long totalSpaceBytes;
    private long usableSpaceBytes;
    private long freeSpaceBytes;
}
