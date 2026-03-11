package com.finly.backend.admin.dto;

import com.finly.backend.domain.model.BudgetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AdminBudgetForm {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private BudgetType type;

    @NotNull
    private UUID categoryId;

    @NotNull
    private UUID userId;
}
