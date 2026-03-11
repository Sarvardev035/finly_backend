package com.finly.backend.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AdminDebtForm {
    @NotBlank
    private String debtorName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private UUID userId;

    private String description;
}
