package bbmovie.commerce.payment_orchestrator_service.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void should_accept_valid_iso_currency_and_non_negative_amount() {
        assertDoesNotThrow(() -> new Money(BigDecimal.valueOf(10), "USD"));
    }

    @Test
    void should_reject_negative_amount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.valueOf(-1), "USD"));
    }

    @Test
    void should_reject_invalid_currency_code() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, "INVALID"));
    }

    @Test
    void should_add_money_with_same_currency() {
        Money left = new Money(BigDecimal.valueOf(10), "USD");
        Money right = new Money(BigDecimal.valueOf(2.5), "USD");

        Money result = left.add(right);

        assertEquals(new BigDecimal("12.5"), result.amount());
        assertEquals("USD", result.currency());
    }

    @Test
    void should_subtract_money_with_same_currency() {
        Money left = new Money(BigDecimal.valueOf(10), "USD");
        Money right = new Money(BigDecimal.valueOf(2), "USD");

        Money result = left.subtract(right);

        assertEquals(new BigDecimal("8"), result.amount());
        assertEquals("USD", result.currency());
    }

    @Test
    void should_reject_subtraction_when_result_negative() {
        Money left = new Money(BigDecimal.ONE, "USD");
        Money right = new Money(BigDecimal.TEN, "USD");

        assertThrows(IllegalArgumentException.class, () -> left.subtract(right));
    }

    @Test
    void should_multiply_money_with_non_negative_multiplier() {
        Money base = new Money(BigDecimal.valueOf(10), "USD");

        Money result = base.multiply(new BigDecimal("1.5"));

        assertEquals(new BigDecimal("15.0"), result.amount());
        assertEquals("USD", result.currency());
    }

    @Test
    void should_reject_operations_when_currency_differs() {
        Money usd = new Money(BigDecimal.TEN, "USD");
        Money eur = new Money(BigDecimal.ONE, "EUR");

        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
    }

    @Test
    void should_compare_money_with_same_currency() {
        Money high = new Money(BigDecimal.TEN, "USD");
        Money low = new Money(BigDecimal.ONE, "USD");

        assertTrue(high.isGreaterThan(low));
    }
}

