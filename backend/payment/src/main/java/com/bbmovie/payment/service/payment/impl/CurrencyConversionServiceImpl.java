package com.bbmovie.payment.service.payment.impl;

import com.bbmovie.payment.service.payment.CurrencyConversionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;

@Log4j2
@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateProvider provider;

    public CurrencyConversionServiceImpl() {
        this.provider = MonetaryConversions.getExchangeRateProvider("ECB");
    }

    @Override
    public MonetaryAmount convert(MonetaryAmount amount, String toCurrency) {
        CurrencyConversion conversion = provider.getCurrencyConversion(toCurrency);
        MonetaryAmount converted = amount.with(conversion);
        log.info("Converted {} -> {}", amount, converted);
        return converted;
    }
}