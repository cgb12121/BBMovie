package com.example.common.dtos.nats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {
    private String url;
    private String publicId;
    private String contentType;
    private Long fileSize;

    public FileUploadResult(String url, String publicId) {
        this.url = url;
        this.publicId = publicId;
    }
}