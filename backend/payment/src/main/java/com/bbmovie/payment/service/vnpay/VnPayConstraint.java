package com.bbmovie.payment.service.vnpay;

@SuppressWarnings("squid:S1118")
public class VnPayConstraint {
    public static final String VERSION = "2.1.0";
    public static final String DATE_FORMAT = "yyyyMMddHHmmss";
    public static final String ONLY_SUPPORTED_CURRENCY = "VND";
    public static final String RETURN_URL = "http://localhost:8080/api/payment/vnpay/return";
    public static final String PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String TRANSACTION_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
}