package bbmovie.commerce.payment_gateway.client.dto;

public record PromotionResult(
        boolean applied,
        String promotionId,
        double discountValue,
        String message
) {
}
