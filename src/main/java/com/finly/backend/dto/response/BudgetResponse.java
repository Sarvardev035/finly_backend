package com.finly.backend.dto.response;

import com.finly.backend.domain.model.BudgetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BudgetResponse {
    private UUID id;
    private BudgetType type;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal monthlyLimit;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal percentageUsed;
    private Integer year;
    private Integer month;
    private String userName;
    private LocalDateTime createdAt;
}
