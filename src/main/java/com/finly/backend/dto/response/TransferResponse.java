package com.finly.backend.dto.response;

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
public class TransferResponse {
    private UUID id;
    private UUID fromAccountId;
    private String fromAccountName;
    private UUID toAccountId;
    private String toAccountName;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private String description;
    private LocalDate transferDate;
    private String userName;
    private LocalDateTime createdAt;
}
