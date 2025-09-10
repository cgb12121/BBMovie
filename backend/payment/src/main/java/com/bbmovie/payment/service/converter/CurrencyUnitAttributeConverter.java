package com.bbmovie.payment.service.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

@Converter(autoApply = true)
public class CurrencyUnitAttributeConverter implements AttributeConverter<CurrencyUnit, String> {

    @Override
    public String convertToDatabaseColumn(CurrencyUnit attribute) {
        return attribute != null ? attribute.getCurrencyCode() : null;
    }

    @Override
    public CurrencyUnit convertToEntityAttribute(String dbData) {
        return dbData != null ? Monetary.getCurrency(dbData) : null;
    }
}
