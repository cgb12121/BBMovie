package bbmovie.commerce.payment_gateway.client.dto;

import java.math.BigDecimal;

public record PromotionResult(
        boolean applied,
        String promotionId,
        BigDecimal discountValue,
        String message
) {
}
