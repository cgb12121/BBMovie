package bbmovie.commerce.commerce_contracts.contracts.payment;

public final class PaymentEventTypes {
    public static final String PAYMENT_INITIATED_V1 = "payment.initiated.v1";
    public static final String PAYMENT_SUCCEEDED_V1 = "payment.succeeded.v1";
    public static final String PAYMENT_FAILED_V1 = "payment.failed.v1";
    public static final String PAYMENT_REFUNDED_V1 = "payment.refunded.v1";
    public static final String PAYMENT_STATUS_UPDATED_V1 = "payment.status.updated.v1";

    private PaymentEventTypes() {
    }
}
