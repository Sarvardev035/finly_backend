package com.finly.backend.dto.response;

import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
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
public class DebtResponse {

    private UUID id;
    private String personName;
    private DebtType type;
    private BigDecimal amount;
    private BigDecimal remainingAmount;
    private Currency currency;
    private UUID accountId;
    private String accountName;
    private String description;
    private DebtStatus status;
    private LocalDate dueDate;
    private String userName;
    private LocalDateTime createdAt;
}
