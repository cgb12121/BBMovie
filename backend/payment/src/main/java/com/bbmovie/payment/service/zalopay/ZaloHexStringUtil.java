package com.bbmovie.payment.service.zalopay;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@SuppressWarnings({"squid:S1118", "unused"})
public final class ZaloHexStringUtil {
    private static final byte[] HEX_CHAR_TABLE = {
        (byte) '0', (byte) '1', (byte) '2', (byte) '3',
        (byte) '4', (byte) '5', (byte) '6', (byte) '7',
        (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
        (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public static String byteArrayToHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0x0F];
        }
        return new String(hex, StandardCharsets.UTF_8);
    }

    public static byte[] hexStringToByteArray(String hex) {
        String hexStandard = hex.toLowerCase(Locale.ENGLISH);
        int sz = hexStandard.length() / 2;
        byte[] bytesResult = new byte[sz];
        int idx = 0;

        for (int i = 0; i < sz; i++) {
            byte first = (byte) hexStandard.charAt(idx++);
            byte second = (byte) hexStandard.charAt(idx++);

            first = (first > HEX_CHAR_TABLE[9])
                ? (byte) (first - ((byte) 'a' - 10))
                : (byte) (first - '0');

            second = (second > HEX_CHAR_TABLE[9])
                ? (byte) (second - ((byte) 'a' - 10))
                : (byte) (second - '0');

            bytesResult[i] = (byte) (first * 16 + second);
        }
        return bytesResult;
    }
}
