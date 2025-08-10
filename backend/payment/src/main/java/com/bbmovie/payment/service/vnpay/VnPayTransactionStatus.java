package com.bbmovie.payment.service.vnpay;

import lombok.Getter;

@Getter
@SuppressWarnings("all")
public enum VnPayTransactionStatus {

    // --- vnp_TransactionStatus codes ---
    SUCCESS("00", "Giao dịch thành công"),
    PENDING("01", "Giao dịch chưa hoàn tất"),
    FAILED("02", "Giao dịch bị lỗi"),
    REVERSED("04", "Giao dịch đảo (trừ tiền nhưng chưa thành công ở VNPAY)"),
    REFUND_PROCESSING("05", "VNPAY đang xử lý giao dịch này (hoàn tiền)"),
    REFUND_SENT_TO_BANK("06", "VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng"),
    FRAUD("07", "Giao dịch bị nghi ngờ gian lận"),
    REFUND_REJECTED("09", "Giao dịch hoàn trả bị từ chối"),

    // --- vnp_ResponseCode codes from Parameters ---
    RESP_SUCCESS("00", "Giao dịch thành công"),
    RESP_FRAUD("07", "Trừ tiền thành công, giao dịch bị nghi ngờ"),
    RESP_NOT_REGISTERED("09", "Chưa đăng ký InternetBanking"),
    RESP_INVALID_INFO("10", "Sai thông tin thẻ/tài khoản quá 3 lần"),
    RESP_TIMEOUT("11", "Hết hạn chờ thanh toán"),
    RESP_ACCOUNT_LOCKED("12", "Tài khoản bị khóa"),
    RESP_INVALID_OTP("13", "Sai OTP"),
    RESP_USER_CANCEL("24", "Khách hàng hủy giao dịch"),
    RESP_INSUFFICIENT_FUNDS("51", "Không đủ số dư"),
    RESP_LIMIT_EXCEEDED("65", "Quá hạn mức giao dịch trong ngày"),
    RESP_BANK_MAINTENANCE("75", "Ngân hàng đang bảo trì"),
    RESP_WRONG_PASSWORD_LIMIT("79", "Sai mật khẩu thanh toán quá số lần quy định"),
    RESP_OTHER_ERROR("99", "Lỗi không xác định");

    private final String code;
    private final String message;

    VnPayTransactionStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static VnPayTransactionStatus fromCode(String code) {
        for (VnPayTransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getMessageFromCode(String code) {
        for (VnPayTransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status.message;
            }
        }
        return null;
    }
}