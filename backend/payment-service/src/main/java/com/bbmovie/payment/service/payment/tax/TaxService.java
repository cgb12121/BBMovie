package com.bbmovie.payment.service.payment.tax;

import com.bbmovie.payment.dto.request.TaxRateRequest;
import java.math.BigDecimal;

public interface TaxService {
    BigDecimal taxRate(TaxRateRequest request);

    BigDecimal calculate();
}