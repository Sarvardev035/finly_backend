package com.finly.backend.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AdminExpenseForm {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String description;

    @NotNull
    private LocalDate expenseDate;

    @NotNull
    private UUID categoryId;

    @NotNull
    private UUID accountId;

    @NotNull
    private UUID userId;
}
