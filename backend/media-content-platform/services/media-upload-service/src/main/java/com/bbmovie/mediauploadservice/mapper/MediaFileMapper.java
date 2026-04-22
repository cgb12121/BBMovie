package com.bbmovie.mediauploadservice.mapper;

import com.bbmovie.mediauploadservice.dto.MediaFileResponse;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import org.springframework.stereotype.Component;

@Component
public class MediaFileMapper {

    public MediaFileResponse toResponse(MediaFile file) {
        if (file == null) {
            return null;
        }
        return MediaFileResponse.builder()
                .uploadId(file.getUploadId())
                .originalFilename(file.getOriginalFilename())
                .purpose(file.getPurpose())
                .status(file.getStatus())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .rejectReason(file.getRejectReason())
                .createdAt(file.getCreatedAt())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
