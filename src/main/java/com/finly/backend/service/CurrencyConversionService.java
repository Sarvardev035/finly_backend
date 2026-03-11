package com.finly.backend.service;

import com.finly.backend.domain.model.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private final ExchangeRateService exchangeRateService;

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (from == null || to == null || from == to) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal rate = exchangeRateService.getRate(from, to);
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }
}
