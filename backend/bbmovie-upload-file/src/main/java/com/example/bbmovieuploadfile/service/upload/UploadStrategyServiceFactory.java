package com.example.bbmovieuploadfile.service.upload;

import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.common.enums.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UploadStrategyServiceFactory {

    private final Map<String, FileUploadStrategyService> strategies;

    @Autowired
    public UploadStrategyServiceFactory(Map<String, FileUploadStrategyService> strategies) {
        this.strategies = strategies;
    }

    public FileUploadStrategyService getService(Storage storage) {
        return switch (storage) {
            case LOCAL -> strategies.get(Storage.LOCAL.name());
            case CLOUDINARY -> strategies.get(Storage.CLOUDINARY.name());
            default -> throw new FileUploadException("Unsupported storage!");
        };
    }
}
