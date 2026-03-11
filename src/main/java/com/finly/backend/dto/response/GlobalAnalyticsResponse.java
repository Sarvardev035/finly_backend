package com.finly.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GlobalAnalyticsResponse {
    private long totalUsers;
    private long totalTransactions;
    private BigDecimal totalSystemBalance;
    private long activeBudgets;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
}
