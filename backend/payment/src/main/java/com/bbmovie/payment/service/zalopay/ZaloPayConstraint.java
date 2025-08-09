package com.bbmovie.payment.service.zalopay;

public class ZaloPayConstraint {
    public static final String ONLY_SUPPORTED_CURRENCY = "VND";

    // Endpoints
    public static final String CREATE_ORDER_URL_SANDBOX = "https://sandbox.zalopay.com.vn/v001/tpe/createorder";
    public static final String CREATE_ORDER_URL_PROD = "https://zalopay.com.vn/v001/tpe/createorder";

    // Callback field names for ZaloPay webhook
    public static final String CALLBACK_DATA = "data";
    public static final String CALLBACK_MAC = "mac";

    // Vietnam timezone for apptransid prefix (yymmdd)
    public static final String VIETNAM_TZ = "Asia/Ho_Chi_Minh";
}
