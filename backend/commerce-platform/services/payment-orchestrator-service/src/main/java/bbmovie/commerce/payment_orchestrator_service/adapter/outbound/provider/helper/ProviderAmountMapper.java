package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProviderAmountMapper {

    private ProviderAmountMapper() {
    }

    public static long toStripeAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    public static String toPaypalAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

