package bbmovie.commerce.billing_ledger_service.domain;

public enum LedgerEntryType {
    PAYMENT_INITIATED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    PAYMENT_STATUS_UPDATED
}
