package com.finly.backend.dto.response;

import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.ExchangeRateSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponse {
    private UUID id;
    private Currency baseCurrency;
    private Currency targetCurrency;
    private BigDecimal rate;
    private ExchangeRateSource source;
    private LocalDateTime createdAt;
}
