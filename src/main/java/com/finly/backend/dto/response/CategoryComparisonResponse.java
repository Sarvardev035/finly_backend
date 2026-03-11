package com.finly.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryComparisonResponse {
    private UUID categoryId;
    private String categoryName;
    private BigDecimal incomeAmount;
    private BigDecimal expenseAmount;
    private BigDecimal netAmount;
}
