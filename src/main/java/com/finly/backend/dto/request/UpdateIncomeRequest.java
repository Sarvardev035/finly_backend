package com.finly.backend.dto.request;

import com.finly.backend.domain.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateIncomeRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private Currency currency;

    private String description;

    private LocalDate incomeDate;

    private UUID categoryId;

    private UUID accountId;
}
