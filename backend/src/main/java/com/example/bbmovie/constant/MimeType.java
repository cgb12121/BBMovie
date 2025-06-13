package com.example.bbmovie.constant;

import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@SuppressWarnings("unused")
public class MimeType {
    public static final String APNG = "image/apng";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String GIF = "image/gif";
    public static final String BMP = "image/bmp";
    public static final String SVG = "image/svg+xml";
    public static final String ICO = "image/x-icon";
    public static final String TIF = "image/tiff";
    public static final String WEBP = "image/webp";
    public static final String AVIF = "image/avif";

    public static final String WEBM = "video/webm";
    public static final String MP4 = "video/mp4";

    private MimeType() {}

    private static final List<String> ALLOWED_CONTENT_TYPES;

    static {
        ALLOWED_CONTENT_TYPES = new ArrayList<>();
        Field[] fields = MimeType.class.getDeclaredFields();

        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) &&
                Modifier.isStatic(modifiers) &&
                Modifier.isFinal(modifiers) &&
                field.getType().equals(String.class)
            ) {
                try {
                    String value = (String) field.get(null);
                    ALLOWED_CONTENT_TYPES.add(value);
                } catch (IllegalAccessException e) {
                    log.error("Failed to getActiveProvider value of static field", e);
                }
            }
        }
    }

    public static List<String> getAllowedContentTypes() {
        return ALLOWED_CONTENT_TYPES;
    }
}
