package com.finly.backend.admin.dto;

import java.math.BigDecimal;

public record AdminDashboardStats(
        long totalUsers,
        long totalAccounts,
        BigDecimal totalExpenses,
        BigDecimal totalIncomes) {
}
