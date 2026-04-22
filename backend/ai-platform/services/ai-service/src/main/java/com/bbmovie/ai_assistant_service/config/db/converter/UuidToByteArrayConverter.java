package com.bbmovie.ai_assistant_service.config.db.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.nio.ByteBuffer;
import java.util.UUID;

@WritingConverter
public class UuidToByteArrayConverter implements Converter<UUID, byte[]> {
    @Override
    public byte[] convert(UUID source) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putInt(Integer.reverseBytes((int) (source.getMostSignificantBits() >> 32)));
        bb.putShort(Short.reverseBytes((short) (source.getMostSignificantBits() >> 16)));
        bb.putShort(Short.reverseBytes((short) source.getMostSignificantBits()));
        bb.putLong(source.getLeastSignificantBits());
        return bb.array();
    }
}