package bbmovie.commerce.payment_gateway.client;

import bbmovie.commerce.payment_gateway.client.dto.PromotionApplyRequest;
import bbmovie.commerce.payment_gateway.client.dto.PromotionResult;
import bbmovie.commerce.payment_gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PromotionClient {
    private final RestClient.Builder restClientBuilder;
    private final GatewayProperties properties;

    public PromotionResult tryApplyCoupon(PromotionApplyRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            return new PromotionResult(false, null, BigDecimal.ZERO, "No coupon applied");
        }
        RestClient client = restClientBuilder.baseUrl(properties.getPromotion().getBaseUrl()).build();
        try {
            Map<?, ?> response = client.post()
                    .uri("/api/promotions/apply-coupon")
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                return new PromotionResult(false, null, BigDecimal.ZERO, "Promotion response empty");
            }
            Object success = response.get("success");
            if (!(success instanceof Boolean ok) || !ok) {
                return new PromotionResult(false, null, BigDecimal.ZERO, "Coupon rejected");
            }
            Object dataObject = response.get("data");
            if (!(dataObject instanceof Map<?, ?> data)) {
                return new PromotionResult(false, null, BigDecimal.ZERO, "Promotion data missing");
            }
            String promotionId = asString(data.get("promotionId"));
            BigDecimal discountValue = asBigDecimal(data.get("discountValue"));
            String message = asString(response.get("message"));
            return new PromotionResult(true, promotionId, discountValue, message);
        } catch (Exception ex) {
            return new PromotionResult(false, null, BigDecimal.ZERO, "Promotion service unavailable");
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
