package com.example.common.dtos.nats;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResult {
    private String url;
    private String publicId;
}