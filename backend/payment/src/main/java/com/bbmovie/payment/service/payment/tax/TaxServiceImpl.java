package com.bbmovie.payment.service.payment.tax;

import com.bbmovie.payment.dto.request.TaxRateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TaxServiceImpl implements TaxService {

    private final TaxRateService taxRateService;

    @Autowired
    public TaxServiceImpl(TaxRateService taxRateService) {
        this.taxRateService = taxRateService;
    }

    @Override
    public BigDecimal taxRate(TaxRateRequest request) {
        return taxRateService.taxRate(request);
    }

    @Override
    public BigDecimal calculate(TaxRateRequest request) {
        // For now, calculation equals the effective tax rate percentage resolved from the API
        // Consumers compute the tax amount as price * (percent / 100)
        return taxRate(request);
    }
}