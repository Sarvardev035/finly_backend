package com.finly.backend.dto.request;

import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.DebtType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateDebtRequest {

    @NotBlank(message = "Person name is required")
    private String personName;

    @NotNull(message = "Debt type is required")
    private DebtType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private Currency currency;

    // Optional account to apply debt/receivable effect
    private UUID accountId;

    private String description;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
