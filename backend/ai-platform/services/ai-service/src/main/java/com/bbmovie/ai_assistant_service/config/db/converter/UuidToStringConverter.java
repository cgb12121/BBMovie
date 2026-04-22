package com.bbmovie.ai_assistant_service.config.db.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

import java.util.UUID;

@WritingConverter
public enum UuidToStringConverter implements Converter<UUID, String> {
    INSTANCE;

    @Override
    public String convert(@NonNull UUID source) {
        return source.toString();
    }
}
