package com.example.bbmovie.service.payment.strategy;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.config.VNPayConfig;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.utils.IpAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component("VnPay")
public class VnPayService implements PaymentStrategy {

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.VN_PAY;
    }

    // (vnp_Command=pay)
    @Override
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = IpAddressUtils.getClientIp(httpServletRequest);
        String vnp_TmnCode = VNPayConfig.vnp_tmn_code;
        String orderType = "order-type";
        String totalCost = String.valueOf(request.getAmount().multiply(new BigDecimal(100)).longValue());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", totalCost);
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        //need to fix this
        vnp_Params.put("vnp_OrderInfo", String.valueOf(request));
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_return_url);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<String>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_secure_hash, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
        return new PaymentResponse(paymentUrl, "PENDING");
    }
}
