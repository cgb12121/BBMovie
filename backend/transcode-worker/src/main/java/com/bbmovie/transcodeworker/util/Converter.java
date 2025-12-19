package com.bbmovie.transcodeworker.util;

/**
 * Utility class for data format conversions, particularly hex string and byte array conversions.
 * Provides static methods for converting between byte arrays and hexadecimal string representations.
 */
public class Converter {

    /** Private constructor to prevent instantiation of utility class */
    private Converter() {}

    /**
     * Converts a byte array to its hexadecimal string representation.
     * Each byte is converted to a two-digit hex value with lowercase letters.
     *
     * @param bytes the byte array to convert
     * @return a hexadecimal string representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array.
     * The input string should contain an even number of hexadecimal characters.
     *
     * @param hex the hexadecimal string to convert
     * @return a byte array representation of the hexadecimal string
     */
    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
