package bbmovie.commerce.payment_gateway.client.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrchestratorCheckoutRequest(
        String userId,
        String userEmail,
        String provider,
        BigDecimal amount,
        String currency,
        String purpose,
        Map<String, String> metadata
) {
}
