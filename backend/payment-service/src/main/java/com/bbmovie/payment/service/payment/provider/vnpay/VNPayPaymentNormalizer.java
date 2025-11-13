package com.bbmovie.payment.service.payment.provider.vnpay;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.VNPayException;
import com.bbmovie.payment.service.PaymentNormalizer;
import org.springframework.stereotype.Component;

import static com.bbmovie.payment.entity.enums.PaymentStatus.*;

@Component("vnpayNormalizer")
public class VNPayPaymentNormalizer implements PaymentNormalizer {
    @Override
    public PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus) {
        return switch (providerStatus.toString()) {
            case "00" -> new PaymentStatus.NormalizedPaymentStatus(SUCCEEDED, "Giao dịch thành công");
            case "07" -> new PaymentStatus.NormalizedPaymentStatus(SUCCEEDED, "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).");
            case "09" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.");
            case "10" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.");
            case "11" -> new PaymentStatus.NormalizedPaymentStatus(CANCELLED, "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.");
            case "12" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.");
            case "13" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch.");
            case "24" -> new PaymentStatus.NormalizedPaymentStatus(CANCELLED, "Giao dịch không thành công do: Khách hàng hủy giao dịch.");
            case "51" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.");
            case "65" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.");
            case "75" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Ngân hàng thanh toán đang bảo trì.");
            case "79" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch.");
            case "99" -> new PaymentStatus.NormalizedPaymentStatus(FAILED, "Lỗi giao dịch thất thường.");
            default ->  throw new VNPayException("Unable to process payment.");
        };
    }
}
