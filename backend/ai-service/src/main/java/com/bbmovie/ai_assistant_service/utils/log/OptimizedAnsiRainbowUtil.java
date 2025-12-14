package com.bbmovie.ai_assistant_service.utils.log;

/**
 * A dev-toy for colorful logging
 * @author Gemini
 */
public class OptimizedAnsiRainbowUtil {

    private static final String ESC = "\u001B";
    private static final String CSI = ESC + "[";
    private static final String RESET = CSI + "0m";
    private static final String UNDERLINE = CSI + "4m";
    // Header cho TrueColor (24-bit): \u001B[38;2;R;G;Bm
    private static final String RGB_HEADER = CSI + "38;2;";

    private OptimizedAnsiRainbowUtil() {}

    // --- Public API ---

    public static String getFullRainbow(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcFullRgb, false);
    }

    public static String getFullRainbowUnderlined(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcFullRgb, true);
    }

    public static String getLightRainbow(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcLightRgb, false);
    }

    public static String getLightRainbowUnderlined(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcLightRgb, true);
    }

    public static String getErrorLightRed(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcErrorRgb, false);
    }

    public static String getErrorLightRedUnderlined(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcErrorRgb, true);
    }

    public static String getWarningOrangeToYellow(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcWarningRgb, false);
    }

    public static String getWarningOrangeToYellowUnderlined(String text) {
        return buildRainbowString(text, OptimizedAnsiRainbowUtil::calcWarningRgb, true);
    }

    // --- Core Optimized Implementation ---
    /**
     * Functional interface chuyên biệt để tránh Boxing/Unboxing (int -> Integer)
     */
    @FunctionalInterface
    private interface ColorCalculator {
        void calculate(int index, int totalLength, int[] rgbContainer);
    }

    private static String buildRainbowString(String text, ColorCalculator calculator, boolean underlined) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int len = text.length();

        // Tối ưu 1: Pre-allocate StringBuilder.
        // 1 char text tốn khoảng 19-25 chars cho mã màu ANSI.
        StringBuilder sb = new StringBuilder(len * 22);

        // Tối ưu 2: Tái sử dụng mảng int[] để không cấp phát bộ nhớ mới trong vòng lặp
        int[] rgb = new int[3];

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            // Xử lý xuống dòng: Reset màu trước khi xuống dòng để tránh lỗi hiển thị trên một số terminal
            if (c == '\n') {
                sb.append(RESET).append(c);
                continue;
            }

            // Tính toán màu (ghi đè vào mảng rgb cũ)
            calculator.calculate(i, len, rgb);

            // Tối ưu 3: Ghi Raw ANSI string thay vì dùng thư viện
            // Format: ESC[38;2;R;G;Bm
            sb.append(RGB_HEADER)
                    .append(rgb[0]).append(';')
                    .append(rgb[1]).append(';')
                    .append(rgb[2]).append('m');

            if (underlined) {
                sb.append(UNDERLINE);
            }

            sb.append(c);
            // Tối ưu 4: KHÔNG append RESET sau mỗi ký tự.
            // Màu ký tự sau sẽ tự động ghi đè ký tự trước. Chỉ reset khi kết thúc chuỗi.
        }

        // Reset màu ở cuối cùng
        sb.append(RESET);
        return sb.toString();
    }

    // --- Math Optimization ---

    private static void calcFullRgb(int index, int total, int[] out) {
        double pos = (double) index / total;
        double angle = pos * 2 * Math.PI; // Tính 1 lần

        // Dùng 127.5 thay vì 127 + 128 để phân phối đều hơn
        out[0] = clamp((int) (Math.sin(angle) * 127 + 128));
        out[1] = clamp((int) (Math.sin(angle + 2 * Math.PI / 3) * 127 + 128));
        out[2] = clamp((int) (Math.sin(angle + 4 * Math.PI / 3) * 127 + 128));
    }

    private static void calcLightRgb(int index, int total, int[] out) {
        double pos = (double) index / total;
        double angle = pos * 2 * Math.PI;

        out[0] = clamp((int) (Math.sin(angle) * 50 + 205));
        out[1] = clamp((int) (Math.sin(angle + 2 * Math.PI / 3) * 50 + 205));
        out[2] = clamp((int) (Math.sin(angle + 4 * Math.PI / 3) * 50 + 205));
    }

    private static void calcErrorRgb(int index, int total, int[] out) {
        double pos = (double) index / Math.max(1, total);
        double angle = pos * 2 * Math.PI;

        out[0] = clamp((int) (255 - 10 * Math.sin(angle)));
        out[1] = clamp((int) (140 + 20 * Math.sin(angle + Math.PI / 3)));
        out[2] = clamp((int) (140 + 15 * Math.sin(angle + Math.PI / 6)));
    }

    private static void calcWarningRgb(int index, int total, int[] out) {
        // Linear interpolation (Lerp) - Nhanh hơn sin/cos
        double t = total <= 1 ? 0.0 : (double) index / (total - 1);

        out[0] = 255; // Red giữ nguyên
        // Green: 220 -> 190
        out[1] = (int) (220 - (30 * t));
        // Blue: 120 -> 0
        out[2] = (int) (120 - (120 * t));
    }

    private static int clamp(int val) {
        if (val < 0) return 0;
        if (val > 255) return 255;
        return val;
    }

    public static String createCenteredHeader(String text, int maxWidth) {
        if (text == null) return "";
        int len = text.length();
        if (len >= maxWidth) return text;

        int padding = maxWidth - len;
        int left = padding / 2;
        // Dùng repeat (Java 11+) nhanh hơn loop
        return " ".repeat(left) + text + " ".repeat(padding - left);
    }
}
