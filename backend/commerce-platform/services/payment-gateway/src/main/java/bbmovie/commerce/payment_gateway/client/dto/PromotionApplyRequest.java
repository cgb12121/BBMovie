package bbmovie.commerce.payment_gateway.client.dto;

import java.math.BigDecimal;

public record PromotionApplyRequest(
        String code,
        String userId,
        BigDecimal cartValue
) {
}
