package com.example.bbmovie.service.payment.callback;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class VNPayCallbackHandler implements PaymentCallbackHandler {

    @Override
    public PaymentProviderType getProviderType() {
        return PaymentProviderType.VN_PAY;
    }

    @Override
    public void handleCallback(String body, Map<String, String> queryParams, HttpServletRequest request) {

    }

    @Override
    public ResponseEntity<?> handleReturn(Map<String, String> queryParams) {
        Map<String, Object> fields = new HashMap<>();
        for (Enumeration<String> params = queryParams.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        String signValue = VNPayConfig.hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {

            } else {

            }
        } else {

        }
    }
}
