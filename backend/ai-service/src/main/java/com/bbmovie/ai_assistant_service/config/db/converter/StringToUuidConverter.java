package com.bbmovie.ai_assistant_service.config.db.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.util.UUID;

@ReadingConverter
public enum StringToUuidConverter implements Converter<String, UUID> {
    INSTANCE;

    @Override
    public UUID convert(@NonNull String source) {
        return source.isBlank() ? null : UUID.fromString(source);
    }
}