package com.bbmovie.payment.service.payment;

import javax.money.MonetaryAmount;

public interface CurrencyConversionService {
    MonetaryAmount convert(MonetaryAmount amount, String toCurrency);
}
