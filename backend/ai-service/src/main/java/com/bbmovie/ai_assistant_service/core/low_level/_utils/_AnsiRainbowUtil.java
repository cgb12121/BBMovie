package com.bbmovie.ai_assistant_service.core.low_level._utils;

import org.fusesource.jansi.Ansi;
import java.util.function.BiFunction;

import static org.fusesource.jansi.Ansi.Attribute.UNDERLINE;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Utility class for creating rainbow-colored ANSI console text.
 * This class is non-instantiable.
 */
public final class _AnsiRainbowUtil {

    private _AnsiRainbowUtil() {}

    /**
     * Returns a string with a full-spectrum rainbow effect.
     */
    public static String getFullRainbow(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getFullRgbColor, false);
    }

    /**
     * Returns a string with a full-spectrum rainbow effect and underline.
     */
    public static String getFullRainbowUnderlined(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getFullRgbColor, true);
    }

    /**
     * Returns a string with a light/pastel-colored rainbow effect.
     */
    public static String getLightRainbow(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getLightRgbColor, false);
    }

    /**
     * Returns a string with a light/pastel-colored rainbow effect and underline.
     */
    public static String getLightRainbowUnderlined(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getLightRgbColor, true);
    }

    /**
     * Returns a string with a light red color useful for errors.
     */
    public static String getErrorLightRed(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getErrorRgb, false);
    }

    /**
     * Returns a string with a light red color and underline useful for errors.
     */
    public static String getErrorLightRedUnderlined(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getErrorRgb, true);
    }

    /**
     * Returns a string with an orange-to-yellow gradient useful for warnings.
     */
    public static String getWarningOrangeToYellow(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getWarningRgb, false);
    }

    /**
     * Returns a string with an orange-to-yellow gradient and underline useful for warnings.
     */
    public static String getWarningOrangeToYellowUnderlined(String text) {
        return buildRainbowString(text, _AnsiRainbowUtil::getWarningRgb, true);
    }


    // --- Private Implementation ---

    /**
     * Core logic to build the ANSI string.
     *
     * @param text          The text to color.
     * @param colorSupplier A function to getWithCursor the {r, g, b} array.
     * @param underlined    Whether to apply the underline attribute.
     */
    private static String buildRainbowString(
            String text, BiFunction<Integer, Integer, int[]> colorSupplier, boolean underlined) {
        StringBuilder sb = new StringBuilder();
        if (text == null) {
            return sb.toString();
        }
        int textLength = text.length();

        for (int i = 0; i < textLength; i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                sb.append(c);
                continue;
            }

            int[] rgb = colorSupplier.apply(i, textLength);
            Ansi chain = ansi().fgRgb(rgb[0], rgb[1], rgb[2]);

            if (underlined) {
                chain.a(UNDERLINE);
            }

            sb.append(chain.a(c).reset());
        }
        return sb.toString();
    }

    /**
     * Calculates a full-spectrum (bright and dark) RGB color.
     */
    private static int[] getFullRgbColor(int index, int totalLength) {
        double position = (double) index / totalLength;
        // This logic (127 + 128) gives a full 0-255 range.
        int red = (int) (Math.sin(position * Math.PI * 2 + 0) * 127 + 128);
        int green = (int) (Math.sin(position * Math.PI * 2 + 2 * Math.PI / 3) * 127 + 128);
        int blue = (int) (Math.sin(position * Math.PI * 2 + 4 * Math.PI / 3) * 127 + 128);
        return new int[]{clamp(red), clamp(green), clamp(blue)};
    }

    /**
     * Calculates a light/pastel RGB color.
     */
    private static int[] getLightRgbColor(int index, int totalLength) {
        double position = (double) index / totalLength;
        // This logic (50 + 205) keeps the range high (155-255),
        // resulting in lighter, pastel-like colors.
        int red = (int) (Math.sin(position * Math.PI * 2 + 0) * 50 + 205);
        int green = (int) (Math.sin(position * Math.PI * 2 + 2 * Math.PI / 3) * 50 + 205);
        int blue = (int) (Math.sin(position * Math.PI * 2 + 4 * Math.PI / 3) * 50 + 205);
        return new int[]{clamp(red), clamp(green), clamp(blue)};
    }

    /**
     * Pastel/light red for errors. Slight per-character variation to avoid a flat look.
     * Keeps red high and green/blue low.
     */
    private static int[] getErrorRgb(int index, int totalLength) {
        double position = (double) index / Math.max(1, totalLength);
        // Base pastel red ~ (255, 140, 140). Add a small sine variation for warmth.
        int red = (int) (255 - 10 * Math.sin(position * Math.PI * 2));
        int green = (int) (140 + 20 * Math.sin(position * Math.PI * 2 + Math.PI / 3));
        int blue = (int) (140 + 15 * Math.sin(position * Math.PI * 2 + Math.PI / 6));
        return new int[]{clamp(red), clamp(green), clamp(blue)};
    }

    /**
     * Gradient from orange to yellow for warnings.
     * Orange approximately (255,140,0) → Yellow approximately (255,215,0).
     */
    private static int[] getWarningRgb(int index, int totalLength) {
        double position = totalLength <= 1 ? 0.0 : (double) index / (totalLength - 1);
        int red = 255;
        int green = (int) (140 + (215 - 140) * position); // interpolate 140 -> 215
        int blue = 0;
        return new int[]{clamp(red), clamp(green), clamp(blue)};
    }

    /**
     * Ensures rgb values stay within 0-255.
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static String createCenteredHeader(String text, int maxWidth) {
        if (text.length() >= maxWidth) {
            return text;
        }
        int padding = maxWidth - text.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}
