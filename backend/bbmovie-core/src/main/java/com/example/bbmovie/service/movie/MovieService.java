package com.example.bbmovie.service.movie;

import com.example.common.dtos.kafka.FileUploadEvent;

public interface MovieService {

    void handleFileUpload(FileUploadEvent event);
}
