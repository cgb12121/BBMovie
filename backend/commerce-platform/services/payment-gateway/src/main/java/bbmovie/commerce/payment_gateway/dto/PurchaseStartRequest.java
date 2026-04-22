package bbmovie.commerce.payment_gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record PurchaseStartRequest(
        @NotBlank String userId,
        @Email String userEmail,
        @NotBlank String provider,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String purpose,
        String couponCode,
        Map<String, String> metadata
) {
}
