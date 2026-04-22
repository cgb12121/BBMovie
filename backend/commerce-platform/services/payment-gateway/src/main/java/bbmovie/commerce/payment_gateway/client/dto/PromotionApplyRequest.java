package bbmovie.commerce.payment_gateway.client.dto;

public record PromotionApplyRequest(
        String code,
        String userId,
        Double cartValue
) {
}
