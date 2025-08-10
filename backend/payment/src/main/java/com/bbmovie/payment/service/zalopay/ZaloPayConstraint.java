package com.bbmovie.payment.service.zalopay;

@SuppressWarnings("all")
public class ZaloPayConstraint {
    public static final String ONLY_SUPPORTED_CURRENCY = "VND";

    // Endpoints
    public static final String CREATE_ORDER_URL_SANDBOX_V1 = "https://sandbox.zalopay.com.vn/v001/tpe/createorder";
    public static final String CREATE_ORDER_URL_PROD_V1 = "https://zalopay.com.vn/v001/tpe/createorder";

    public static final String CREATE_ORDER_URL_SANDBOX_V2 = "https://sb-openapi.zalopay.vn/v2/create";
    public static final String CREATE_ORDER_URL_PROD_V2 = "https://openapi.zalopay.vn/v2/create";

    // Callback field names for ZaloPay webhook
    public static final String CALLBACK_DATA = "data";
    public static final String CALLBACK_MAC = "mac";

    // Vietnam timezone for apptransid prefix (yymmdd)
    public static final String VIETNAM_TZ = "Asia/Ho_Chi_Minh";
}