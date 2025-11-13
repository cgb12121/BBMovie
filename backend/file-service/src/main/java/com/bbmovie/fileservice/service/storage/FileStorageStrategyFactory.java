package com.bbmovie.fileservice.service.storage;

import com.bbmovie.common.enums.Storage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileStorageStrategyFactory {

    private final Map<String, StorageStrategy> strategies;

    public FileStorageStrategyFactory(List<StorageStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(StorageStrategy::getStorageType, s -> s));
    }

    public StorageStrategy getStrategy(String storageType) {
        return strategies.getOrDefault(storageType.toUpperCase(), strategies.get(Storage.LOCAL.name()));
    }
}
