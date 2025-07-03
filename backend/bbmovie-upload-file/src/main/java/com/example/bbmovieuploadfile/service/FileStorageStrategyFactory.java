package com.example.bbmovieuploadfile.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileStorageStrategyFactory {

    private final Map<String, FileStorageStrategy> strategies;

    public FileStorageStrategyFactory(List<FileStorageStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FileStorageStrategy::getStorageType, s -> s));
    }

    public FileStorageStrategy getStrategy(String storageType) {
        return strategies.getOrDefault(storageType.toUpperCase(), strategies.get("localStorage"));
    }
}
