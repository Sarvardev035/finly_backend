package com.finly.backend.dto.response;

import com.finly.backend.domain.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseResponse {
    private UUID id;
    private BigDecimal amount;
    private Currency currency;
    private String description;
    private LocalDate expenseDate;
    private UUID categoryId;
    private String categoryName;
    private UUID accountId;
    private String accountName;
    private String userName;
    private LocalDateTime createdAt;
}
