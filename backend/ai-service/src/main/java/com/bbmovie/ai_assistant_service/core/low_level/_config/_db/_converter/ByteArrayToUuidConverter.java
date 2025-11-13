package com.bbmovie.ai_assistant_service.core.low_level._config._db._converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

@ReadingConverter
public class ByteArrayToUuidConverter implements Converter<byte[], UUID> {
    @Override
    public UUID convert(@NonNull byte[] source) {
        ByteBuffer bb = ByteBuffer.wrap(source);
        long high = Integer.toUnsignedLong(Integer.reverseBytes(bb.getInt())) << 32;
        high |= Short.toUnsignedLong(Short.reverseBytes(bb.getShort())) << 16;
        high |= Short.toUnsignedLong(Short.reverseBytes(bb.getShort()));
        long low = bb.getLong();
        return new UUID(high, low);
    }
}
