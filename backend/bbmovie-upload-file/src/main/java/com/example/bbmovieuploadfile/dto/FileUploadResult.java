package com.example.bbmovieuploadfile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResult {
    private String url;
    private String publicId;
}