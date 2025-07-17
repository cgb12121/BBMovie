package com.example.bbmoviestream.service;

import com.example.common.enums.Storage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VideoStreamingStrategyFactory {

    private final Map<Storage, VideoStreamingStrategy> strategyMap;

    public VideoStreamingStrategyFactory(List<VideoStreamingStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(VideoStreamingStrategy::getStorageType, Function.identity()));
    }

    public VideoStreamingStrategy getStrategy(Storage storageType) {
        return Optional.ofNullable(strategyMap.get(storageType))
                .orElseThrow(() -> new IllegalArgumentException("No strategy for storage: " + storageType));
    }
}
