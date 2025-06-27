package com.example.bbmovie.service.movie;

import com.example.bbmovie.dto.kafka.consumer.FileUploadEvent;

public interface MovieService {

    void handleFileUpload(FileUploadEvent event);
}
