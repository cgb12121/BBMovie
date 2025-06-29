package com.example.bbmovieuploadfile.dto;

import com.example.bbmovieuploadfile.enums.EntityType;
import com.example.bbmovieuploadfile.enums.FileType;
import com.example.bbmovieuploadfile.enums.Storage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {
    private FileType fileType;    // POSTER, VIDEO, TRAILER
    private EntityType entityType;  // MOVIE, ACTOR, DIRECTOR
    private Storage storage;     // LOCAL, CLOUDINARY
    private String quality;     // 720P, 1080P, optional
}