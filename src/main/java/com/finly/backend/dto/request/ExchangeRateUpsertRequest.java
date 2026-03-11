package com.finly.backend.dto.request;

import com.finly.backend.domain.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateUpsertRequest {

    @NotNull(message = "Base currency is required")
    private Currency baseCurrency;

    @NotNull(message = "Target currency is required")
    private Currency targetCurrency;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.00000001", message = "Rate must be greater than zero")
    private BigDecimal rate;
}
